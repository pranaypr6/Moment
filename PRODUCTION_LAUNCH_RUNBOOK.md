# Moment — Production Launch Runbook

Everything needed to take Moment from "code is ready" to "live on the Play Store," in order. Each section only needs to be done once unless noted. Check items off as you go — nothing here is generic boilerplate, it's built from your actual stack (Railway + Postgres/EF Core backend, Cloudflare R2, Firebase, Android/Kotlin client).

---

## 0. Decisions to make before you start

- [ ] **JWT key rotation** — from the earlier conversation: rotate `Jwt:Key` now for a clean slate, or let the one old leaked token expire naturally on 2026-08-08. Either is fine; just decide before Step 2 so you're not doing it twice.
- [ ] **Staged rollout %** — Play Console lets you release to a percentage of users first (recommended: 20% → 50% → 100% over a few days) rather than 100% on day one. Decide now so you're not improvising at submission time.

---

## 1. Backend: environment variables (Railway)

Confirm every one of these is set as a **Railway environment variable** on the production service before deploying (not just in a local `appsettings.Development.json`, which is gitignored and won't exist on Railway). ASP.NET Core reads nested config keys from env vars using double underscores (`Section__Key`), except the two that `Program.cs` reads as flat names.

| Variable (exact name Railway needs) | What it is | Where it's used |
|---|---|---|
| `DATABASE_URL` | Full Postgres connection string (Railway's Postgres plugin provides this automatically if you're using their managed Postgres) | `Program.cs:62` |
| `Jwt__Key` | JWT signing secret — **this is the one to rotate if you chose to in Step 0** | `Program.cs:188` |
| `Jwt__Issuer` | Should be `moment-api` | `Program.cs:185` |
| `Jwt__Audience` | Should be `moment-app` | `Program.cs:186` |
| `GoogleClientId` | Your OAuth web client ID (`688779812269-gcb11o9051vr2v1q3bpjc9jt8j7bcms5.apps.googleusercontent.com`) | Google Sign-In token validation |
| `Cloudflare__AccountId` | Your Cloudflare account ID | `StorageService.cs:26` |
| `Cloudflare__AccessKeyId` | R2 API access key | `StorageService.cs:24` |
| `Cloudflare__SecretAccessKey` | R2 API secret key | `StorageService.cs:25` |
| `Cloudflare__BucketName` | `moment-assets` | `StorageService.cs:38` |
| `Cloudflare__PublicUrl` | Only used as a legacy fallback now that presigned URLs are the default — safe to leave as-is | `StorageService.cs:56` |
| `FIREBASE_CREDENTIALS_BASE64` | Base64-encoded Firebase Admin SDK service account JSON (real value currently sits in your local, gitignored `appsettings.Development.json` — copy that exact value to Railway, don't regenerate a new one unless you want to) | `Program.cs:87-88` |

**Action:**
- [ ] Log into Railway, open the backend service → Variables tab, and confirm every row above is present and correct.
- [ ] If you decided to rotate the JWT key in Step 0, generate a new random 32+ byte secret now (`openssl rand -base64 48` on any machine with OpenSSL) and set it as `Jwt__Key`. Do this now, in this step, not separately later.

---

## 2. Backend: apply the database migration (release-phase step)

Migrations do **not** run automatically on startup (by design — see `Program.cs`'s comment and `backend/DEPLOY.md`). Skipping this step means the app boots against a schema that doesn't match its code.

- [ ] From a machine with the .NET SDK and `dotnet-ef` installed (not this sandbox — it has neither), run against the **production** connection string:
  ```
  cd backend
  dotnet tool restore
  dotnet ef database update --project Moment.Api.csproj
  ```
- [ ] On Railway specifically: set this as a **Release Command** / pre-deploy step if your plan supports it, so it always runs before the new web process starts taking traffic on every future deploy — not just this one. If your plan doesn't support a release command, run it manually via `railway run <command>` right before promoting each deploy.
- [ ] Confirm it completed with no errors before moving on — if a migration fails partway, do not proceed to Step 3 until it's resolved (check `backend/DEPLOY.md`'s rollback instructions if needed).

---

## 3. Backend: deploy and smoke-test

- [ ] Push/deploy the current `main` branch to Railway (whatever your normal deploy trigger is — a Railway auto-deploy on push, or a manual deploy from the dashboard).
- [ ] Wait for the deploy to go healthy, then smoke-test with a real request that needs no auth:
  ```
  curl "https://moment-production-b0e4.up.railway.app/api/auth/username-available?username=test123"
  ```
  A JSON `true`/`false` response (not a 500 or connection error) confirms the app booted, the DB connection works, and routing is live.
- [ ] Check Railway's logs for the first 2-3 minutes after deploy — you're specifically looking for the `PendingModelChangesWarning` log line (expected, harmless, already downgraded from a crash) and confirming there's no `Npgsql` connection-refused error (would mean `DATABASE_URL` is wrong) or Firebase warning about missing credentials (would mean `FIREBASE_CREDENTIALS_BASE64` didn't get set).
- [ ] If you rotated the JWT key: confirm you can still log in fresh via Google Sign-In from a test device (a rotated key invalidates old tokens but a brand-new login should work immediately — if it doesn't, the new key didn't actually get picked up).

---

## 4. Cloudflare R2: final check

- [ ] Confirm "Public Development URL" is disabled on the `moment-assets` bucket (you already did this — just re-check it's still off, since it's a per-bucket toggle someone could accidentally re-enable).
- [ ] Open a real moment/profile picture URL your app currently returns and confirm it's an `r2.cloudflarestorage.com` presigned URL (with a long query-string signature), not a `pub-*.r2.dev` URL. If you still see `pub-*.r2.dev` anywhere, the backend deploy in Step 3 didn't actually pick up the presigned-URL code — redeploy.

---

## 5. Firebase / FCM: production check

- [ ] Confirm the Firebase project (`moment-cfa3e`) is the one your **release-signed** APK/AAB is registered under in the Firebase console (Project Settings → Your apps → Android app), matching the release keystore's SHA-1/SHA-256 fingerprint, not just the debug one. If only the debug fingerprint is registered, Google Sign-In and FCM can silently fail in the actual Play Store build even though everything works from your debug build.
  - Get your release SHA-1/SHA-256: `keytool -list -v -keystore <your-release-keystore> -alias <your-key-alias>`
  - Add it under Firebase Console → Project Settings → Your apps → the Android app → Add fingerprint.
- [ ] Send yourself a real test push (create a moment, change a vibe) against the production backend and confirm it arrives — this is your only true end-to-end confirmation that `FIREBASE_CREDENTIALS_BASE64` on Railway is valid and correctly scoped to this same Firebase project.

---

## 6. Android: build the signed release artifact

### 6.1 Verify signing is wired correctly
- [ ] Confirm `android/keystore.properties` (already present, gitignored) has real, correct values for `storeFile`, `storePassword`, `keyAlias`, `keyPassword` — this is what makes `signingConfig = signingConfigs.getByName("release")` actually apply in `app/build.gradle.kts`. If any value is wrong, `bundleRelease` will fail loudly (not silently sign with the debug key — that's intentional, per the comment in `build.gradle.kts`).
- [ ] Confirm the keystore file itself exists at the path `storeFile` points to, and back it up somewhere outside this repo right now if you haven't already (a password manager, an encrypted drive, a physical note) — if you ever lose this keystore, you cannot update this app on Play again under the same listing. This is the single most unrecoverable file in this entire project.

### 6.2 Bump the version
- [ ] In `app/build.gradle.kts`, `versionCode` must strictly increase on every single upload to Play Console, including test tracks — it's currently `1`. If this is truly your first ever upload, leave it at `1`. If you've uploaded anything before (even to internal testing), bump it to `2` and bump `versionName` too if you want the visible version string to change (e.g., `1.0.0` → `1.0.1`).

### 6.3 Build clean, not incremental
- [ ] Given the kapt/Hilt staleness issue we already hit once this session (a DI constructor change silently not picked up by an incremental build), do a genuinely clean build for the release artifact, not just `bundleRelease` on top of whatever's cached:
  ```
  cd android
  ./gradlew clean
  ./gradlew bundleRelease
  ```
- [ ] Confirm the output exists at `android/app/build/outputs/bundle/release/app-release.aab`. Play Console requires the **AAB** (Android App Bundle) format for all new apps — it does not accept a plain APK for a first submission.
- [ ] Also generate a release APK for your own device-testing convenience (AABs can't be installed directly via `adb install` — you'd need `bundletool` to extract a usable APK set from an AAB, which is extra friction for a quick manual test):
  ```
  ./gradlew assembleRelease
  ```
  This produces `app/build/outputs/apk/release/app-release.apk`, signed with the same release key, functionally identical to what Play will serve — good enough for a real device test.

### 6.4 Manually test the actual signed release build
- [ ] Install `app-release.apk` on a real device (`adb install app/build/outputs/apk/release/app-release.apk`) — not a debug build. Release builds have `isMinifyEnabled = true` (R8/ProGuard code shrinking and obfuscation), which occasionally breaks things that work fine in an unminified debug build (reflection-based libraries, missing keep rules, resources stripped as "unused" when they're actually referenced dynamically). A debug-only test tells you nothing about whether ProGuard broke something.
- [ ] Walk through every core flow end to end on this exact release build, not just a spot check:
  - [ ] Fresh install → Google Sign-In → onboarding (username, display name, profile picture) → consent checkbox
  - [ ] Create a pairing key, join from a second account/device, confirm both sides see the paired state
  - [ ] Take a photo, edit it, send it as a moment, confirm it arrives and sets wallpaper on the receiving device
  - [ ] Change vibe, change anniversary date, change profile picture — confirm the widget updates on its own (the exact bug fixed earlier this session)
  - [ ] Send a presence signal / reaction from both the in-app UI and the widget
  - [ ] Pause the relationship, unpair, report/block — confirm each completes and the UI reflects it without needing a manual refresh
  - [ ] Logout, then log back in — confirm no stale data from the previous session appears anywhere, including the widget
  - [ ] Force-quit the app entirely (not just background it) and reopen — confirm session persists (no unwanted forced logout) and widget still shows correct data
  - [ ] Deny the camera permission and deny the notification permission at least once each, to see the actual denied-state screens a real user might hit
- [ ] Test on at least two real API levels if you can — your `minSdk` is 26 (Android 8.0) and `targetSdk`/`compileSdk` is 36 (Android 16). If you only have one modern test device, prioritize testing on the oldest OS version you can get access to (an emulator image for API 26-28 is a reasonable substitute for the low end if no old physical device is available) — the splash-screen inconsistency flagged in the audit is specifically an API 26-30 issue you won't see at all on a modern device.

---

## 7. Prepare every store listing asset

None of this exists yet per the last audit — it's entirely outside the codebase, so nothing here was blocked by any of the engineering work. Google will reject the submission if any required asset is missing or doesn't meet spec, so treat every checkbox below as literal, not approximate.

### 7.1 App icon (512×512, the "hi-res icon" Play Console asset)
- [ ] Your actual in-app launcher icon already exists and is a real, finished design — a pink wax-seal with a cursive "M" on a cream background (`app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`, confirmed by viewing the file directly). It is **not** a placeholder.
- [ ] The catch: the largest raster export of that icon currently in the repo is 192×192px (the `xxxhdpi` density bucket). Play Console's hi-res store icon must be exactly **512×512px, 32-bit PNG with alpha, under 1MB**. Upscaling a 192×192 PNG to 512×512 will look visibly soft/blurry at that size, especially on a store listing where it's shown large.
- [ ] Go back to whatever you used to originally design this icon (Figma, Illustrator, a generator tool, etc.) and re-export directly at 512×512 from the original vector/high-res source. Only fall back to upscaling the existing PNG (with a tool like an AI upscaler, used carefully) if the original source is genuinely gone — treat that as a last resort, not the default plan.
- [ ] Also note, purely as cleanup (not blocking): `app/src/main/res/drawable/ic_launcher_background.xml` and `ic_launcher_foreground.xml` are leftover default Android-Studio-template vector files (a plain white square on a flat color) that aren't actually used — the real adaptive icon (`mipmap-anydpi-v26/ic_launcher.xml`) points at the PNG mipmap foreground and a solid `#f6ede4` background color instead. Harmless dead resources, safe to delete whenever, not related to the store icon.

### 7.2 Feature graphic (1024×500, required)
- [ ] Exactly 1024×500px, JPEG or 24-bit PNG, **no transparency**. This is the banner shown at the top of your store listing and in some Play Store promotional placements.
- [ ] Design this fresh at the exact 1024×500 landscape dimensions — don't crop or stretch a screenshot or the app icon into this shape, it reads as low-effort and Google's own guidance explicitly discourages it. Reuse your app's actual palette (the soft cream/rose theme visible in the Login screen and the app icon) so it looks like it belongs to the same product.

### 7.3 Phone screenshots (minimum 2, up to 8)
- [ ] JPEG or 24-bit PNG, no alpha channel, each side between 320px and 3840px, max aspect ratio 2:1. Practically: capture at your device's real resolution (a modern phone will typically give you clean 1080×1920 or similar) rather than resizing after the fact.
- [ ] Capture these from the **release build you already tested in step 6.4** — reuse that same test pass as your screenshot session so you're not doing a separate round of app usage just for pictures. Recommended shots, in this order (first screenshot matters most — it's what shows in search results):
  1. The Moments/timeline screen with a real (or realistic placeholder) photo moment visible
  2. The wallpaper-on-lockscreen result — this is the actual core value proposition of the app, make sure at least one screenshot shows a phone with the wallpaper actually applied
  3. The home-screen widget, placed on an actual home screen, showing partner info/vibe/day-count
  4. The Us/relationship screen (vibe, anniversary, partner info)
  5. The onboarding/pairing flow
  6. Login screen (optional, lowest priority — least visually distinctive)
- [ ] Do not include real private photos of yourself/your partner if you'd rather not — either stage clearly-fictional/stock content for the screenshots, or use content you're comfortable with the whole world seeing on a public store listing, since that's exactly where these go.

### 7.4 Store listing text
- [ ] **App title** — short, exact product name as it'll appear in search (e.g., "Moment — Wallpaper for Couples" or similar; keep it under Play's title length limit, roughly 30 characters).
- [ ] **Short description** — max 80 characters, shows under the title in search results. Something like: "Share photos that become your partner's wallpaper, instantly."
- [ ] **Full description** — up to 4000 characters, shown on the full listing page. Cover: what the app does (pair with your partner, send a photo, it becomes their wallpaper), the widget (live vibe/day-count on the home screen), and privacy/simplicity framing (only shared with your one paired partner, nothing public). Avoid keyword-stuffing — Google's review specifically penalizes that.
- [ ] **App category** — Lifestyle or Social both plausibly fit; pick whichever feels closer to how you'd want it discovered.
- [ ] **Contact details** — a support email (you already have one you use — `pranayburra6@gmail.com` or `pranayburra66@gmail.com`, both confirmed yours) is required; a website URL is optional but you already have one (`moment-cfa3e.web.app` or `momentapp.in` once that's actually working).

---

## 8. Google Play Console: account and app setup

### 8.1 Developer account
- [ ] If you don't already have one: register at `play.google.com/console`, pay the one-time $25 registration fee, and complete identity verification (can take anywhere from under an hour to a few days if manual review is needed — do this as early as possible, don't leave it until the day you want to submit).
- [ ] Note which account type you have: a **personal** account, or an **organization** account (registered as a company with a D-U-N-S number). This directly determines what Step 9 requires of you — see the branch below.

### 8.2 Create the app entry
- [ ] Play Console → "Create app" → enter the app name, select default language, declare it's an app (not a game), and declare free (not paid).
- [ ] Complete the **Declarations** section: export compliance (standard encryption declaration — HTTPS/TLS only, no custom cryptography, this almost always qualifies for the standard exemption), and the US export laws checkbox.

### 8.3 App content section (do all of these — each is a separate mandatory sub-form)
- [ ] **Privacy policy** — paste the live, redeployed URL (`https://moment-cfa3e.web.app/privacy.html`, or your custom domain once confirmed working). Must be publicly reachable with no login wall — Play's automated checker will actually fetch it.
- [ ] **Data safety form** — this is the big one, fill it in field by field based on what Moment actually does (cross-referenced against your real code, not guessed):
  - Does your app collect or share any of the required user data types? **Yes.**
  - **Personal info** → Name (display name), Email address → collected, required, used for account management, not shared with third parties beyond your listed service providers.
  - **Photos and videos** → Photos → collected, required (this is the core feature), used for app functionality, shared only with the user's own paired partner (not with any third party in Google's sense — partner-to-partner sharing within the app isn't "third-party sharing" for this form's purposes, but say so in the optional description field if it lets you clarify).
  - **App activity** → App interactions (presence/vibe signals, in-app actions) → collected, used for app functionality.
  - **Device or other IDs** → Device or other IDs (FCM token) → collected, required, used for app functionality (push delivery).
  - **Diagnostics** → Crash logs, app performance data (via Firebase Crashlytics) → collected, used for analytics.
  - Everything else (Location, Financial info, Health and fitness, Messages, Web browsing history, etc.) → **not collected** — confirmed by checking your `AndroidManifest.xml` permissions directly: no location permission exists (only INTERNET, ACCESS_NETWORK_STATE, SET_WALLPAPER, POST_NOTIFICATIONS, CAMERA, WAKE_LOCK, FOREGROUND_SERVICE, FOREGROUND_SERVICE_DATA_SYNC, VIBRATE), so don't over-declare data types you don't actually touch.
  - Is all data encrypted in transit? **Yes** (HTTPS hardcoded, cleartext disabled app-wide — already verified this session).
  - Do you provide a way for users to request data deletion? **Yes** — link the in-app Delete Account flow and the `delete-account.html` page.
- [ ] **Content rating (IARC) questionnaire** — answer honestly: no violence, no explicit content, no user-generated content visible to strangers (content is only ever visible to one paired partner), no chat with strangers, no gambling. This should land in a low, all-ages-adjacent rating, but only the questionnaire's own output is authoritative — don't assume the result in advance.
- [ ] **Target audience and content** — select the actual adult/general age range this app is for, and explicitly mark that this app is **not** primarily child-directed. Getting this wrong in either direction (marking it as designed for children when it isn't) triggers a much stricter review track (Google Play Families policies) that doesn't apply to a couples' app.
- [ ] **Ads** — declare **No ads** (confirmed nothing in the codebase shows ads).
- [ ] **App access** — this one's easy to miss and directly relevant to Moment specifically: since most of the app's real functionality (moments, widget, vibe, anniversary) only becomes visible **after** two accounts are paired, a reviewer testing with a single fresh account may not be able to see the core feature at all. In this section, either:
  - Provide two working test account credentials (Google Sign-In test accounts) that are already paired with each other, with brief written instructions ("log in as Account A, moments/widget will show data already shared by Account B"), or
  - Provide clear written steps for how a reviewer can pair two accounts themselves in a few minutes if you'd rather not hand over live credentials.
  Skipping this section is one of the more common reasons a perfectly working app gets rejected or heavily delayed — the reviewer literally can't evaluate what they can't access.
- [ ] **Government app / COVID-19 contact tracing / Financial features / News app** declarations — all **No** for Moment.

### 8.4 Store presence
- [ ] Fill in the Main store listing (title, descriptions, icon, feature graphic, screenshots from Step 7).
- [ ] Set **Countries/regions** — select where you want the app available (worldwide is the default and simplest choice unless you have a specific reason to restrict it).
- [ ] Set **Pricing** — Free.

---

## 9. Create the release and get to production

### 9.1 Check which track requirement applies to you
- [ ] **If you have an organization Play Console account, or a personal account created before November 13, 2023:** you can publish straight to a production release after internal testing, no mandatory closed-testing period.
- [ ] **If you have a personal account created on or after November 13, 2023 (this is very likely the case if you just registered one for this launch):** Google requires you to run a **closed test with at least 12 opted-in testers, continuously enrolled for at least 14 days**, before you're allowed to apply for production access for a new app. This is a hard gate, not a suggestion — Play Console won't let you promote to production without it. Confirm which bucket you're in now, in Play Console's own account settings, because this determines your actual timeline: if you're in the 12-testers/14-days bucket, factor at least two weeks into your launch plan starting from when you first publish to closed testing, not from today.

### 9.2 Internal testing (do this regardless of which bucket you're in)
- [ ] Testing → Internal testing → create a release → upload the `.aab` from Step 6.3.
- [ ] Add yourself (and your partner, if testing pairing) as internal testers by email, share the opt-in link, and install via that link — not a sideloaded APK — since this is what exercises Play's own signing/delivery path for the first time.
- [ ] Confirm the whole flow from Step 6.4 still works when installed this way.

### 9.3 Closed testing (only if you're in the 12-testers/14-days bucket from 9.1)
- [ ] Testing → Closed testing → create a track, upload the same (or a newer) `.aab`, add at least 12 real testers by email (friends, family, anyone willing to install and open the app), and make sure they actually opt in and keep the app installed for the full 14 continuous days — Google checks actual opt-in duration, not just that you created the track 14 days ago.
- [ ] Once the 14 days are up, apply for production access from within Play Console (there's an explicit prompt/form for this once eligible). Google's typical review turnaround for this specific request is about a week or less.

### 9.4 Production release
- [ ] Once you're clear to publish to production (either because you were exempt in 9.1, or you completed 9.3), create a Production release, upload the final `.aab`, and set your staged rollout percentage from Step 0 (recommended: start at 20%, not 100%).
- [ ] Submit for review. First-time full app review can take anywhere from a few hours to a few days — don't plan a hard launch moment around an exact time, especially the first time you submit under this account.
- [ ] Check the **Pre-launch report** Play Console generates automatically (it runs your uploaded build on a range of real/virtual devices and flags crashes, ANRs, and security warnings) before or shortly after submitting — it's free signal you'd otherwise only get from real users.

---

## 10. Post-launch monitoring (first 24-48 hours)

- [ ] Watch Firebase Crashlytics for any new crash clusters — this is your earliest and most reliable signal, since it's already wired into the app.
- [ ] Watch Railway's logs/metrics for error-rate or latency spikes, and Railway's Postgres plugin metrics for connection-pool exhaustion under real concurrent load for the first time.
- [ ] Watch Play Console's "Statistics" and "Android vitals" tabs (ANR rate, crash rate) — Google will actively warn/throttle your rollout if vitals are bad, so check this daily during the staged rollout window.
- [ ] Confirm real push notifications (moment sent, vibe changed) are arriving for real users, not just your own test account — FCM token registration issues can be silent (this is exactly the class of bug fixed earlier this session).

---

## Rollback triggers

Decide now, not mid-incident:
- Crash-free rate drops below ~99% in Crashlytics → halt the staged rollout in Play Console (it has a native "halt rollout" button — use it, don't unpublish).
- Backend 5xx rate spikes or the database migration step failed silently → revert the Railway deploy to the previous build; do not attempt a second migration on top of a bad one without investigating first.
- A P0 data-safety issue (e.g., photos visible to the wrong user) → halt rollout immediately and pull the previous build; this class of bug takes priority over everything else.

---

## What's intentionally not in this runbook

The non-blocking punch list from `PRODUCTION_READINESS_REVIEW_V3.md` (accessibility gaps, dead invite-code feature, settings UI divergence, aging dependencies, etc.) — none of it blocks this launch. Treat it as your first fast-follow update after real user feedback starts coming in, prioritized by what users actually complain about rather than guessing in advance.
