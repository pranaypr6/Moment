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

## 6. Android: build the release artifact

- [ ] Confirm `android/keystore.properties` (already present, gitignored) points at your real release keystore and the values are correct — this is what makes `signingConfig = signingConfigs.getByName("release")` actually apply in `app/build.gradle.kts`.
- [ ] Bump `versionCode` in `app/build.gradle.kts` if this isn't your very first upload (must strictly increase on every Play Console upload — currently `1`).
- [ ] Do a full clean build, not incremental (see the earlier widget-debugging conversation — kapt/Hilt staleness is a real risk on a release build too):
  ```
  cd android
  ./gradlew clean bundleRelease
  ```
- [ ] Confirm the output exists at `android/app/build/outputs/bundle/release/app-release.aab` — Play Console requires the **AAB** format, not a plain APK, for new submissions.
- [ ] Install and manually test the actual signed release build (not a debug build) on a real device before uploading — release builds have `isMinifyEnabled = true` (R8/ProGuard), which occasionally breaks things that work fine in debug (reflection-based libraries, missing keep rules). At minimum, walk through: Google Sign-In → onboarding → pairing → send a moment → widget updates → logout → re-login.

---

## 7. Play Console: store listing and compliance

- [ ] **App content / Data Safety form** — mandatory for every app, including yours. Declare every data category Moment actually collects: account info (email, name, profile picture), photos (the moments themselves), device identifiers (FCM tokens), and app activity (presence/vibe signals). Third-party SDKs count as your own collection too — this includes Firebase Analytics/Crashlytics and Google Sign-In, so declare those explicitly rather than only your own backend's data.
- [ ] **Content rating questionnaire (IARC)** — required for the app to be publicly listed at all; takes about 5 minutes. Answer honestly for a couples/relationship app with photo sharing (no explicit content, no user-generated public content, no chat with strangers) — this should land in a low/family-friendly-adjacent rating, but only the questionnaire result is authoritative.
- [ ] **Privacy policy URL** — must be publicly reachable (yours is at `website/public/privacy.html` — confirm it's actually deployed and reachable at a real URL, and put that exact URL into Play Console's app content section). From the earlier reports: this document is still missing a data-retention-period statement and a minimum-age/children's-privacy clause — Google's reviewers/automated Data Safety checks can catch the absence of these, so consider adding them before submission rather than after a rejection.
- [ ] **Target API level** — already satisfied: `compileSdk`/`targetSdk` are both 36, ahead of the Aug 31, 2026 requirement for new app submissions.
- [ ] **Store listing assets** — title, short/full description, feature graphic (1024×500), phone screenshots (minimum 2, real device or emulator captures of actual app screens — not mockups), and an app icon. None of this exists yet per the last audit; it's a Play Console asset task, not a code task, but it blocks submission until done.
- [ ] **App category and target audience** — set an appropriate category (Lifestyle or Social likely fits) and confirm you are **not** marking this as an app "designed for children" (it isn't, and doing so incorrectly triggers a much stricter review track).

---

## 8. Play Console: create the release

- [ ] Create your app in Play Console if you haven't already (App name, default language, app/game, free/paid).
- [ ] Go to **Testing → Internal testing** first (not straight to production) — upload the `.aab`, add yourself and any testers as internal testers, and confirm the whole flow works when installed from an actual Play Store internal-testing link (this is meaningfully different from a sideloaded APK — it exercises Play's own signing and delivery path).
- [ ] Once internal testing confirms it installs and runs cleanly, promote to **Production**, using the staged rollout percentage you decided in Step 0.
- [ ] Submit for review. First-time app review can take anywhere from a few hours to a few days — don't plan a hard launch moment around an exact time.

---

## 9. Post-launch monitoring (first 24-48 hours)

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
