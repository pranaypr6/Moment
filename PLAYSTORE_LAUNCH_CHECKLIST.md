# Moment — Play Store Launch Checklist
Everything left to do, on your machine, in order. I fixed the code; these are the steps only you can do (backups, real builds on your hardware, and anything inside Play Console/Firebase Console).

---

## Phase A — Critical one-time setup (do these first, in this order)

- [ ] **Back up the release keystore right now.** I generated `android/keystore/moment-release.jks` for you. Copy it (and `android/keystore.properties`, which holds its passwords) to at least one place outside this repo — a password manager, an encrypted drive, cloud backup. Neither file is in git (both are gitignored on purpose). If you lose this keystore before enrolling in Play App Signing, you can never update this app again under the same listing.

- [ ] **Register the new keystore's SHA-1 with Firebase (required for Google Sign-In to work in release builds).** The release keystore's fingerprint is different from whatever debug key was registered before, so Google Sign-In will fail in release until you add this:
  - SHA-1: `60:CC:24:3C:59:C7:A7:2B:91:1C:B9:8A:69:E4:A7:A0:20:8E:83:D7`
  - Go to Firebase Console → Project Settings → your Android app (`com.moment.app`) → Add fingerprint → paste the SHA-1 above.
  - Re-download `google-services.json` afterward and replace `android/app/google-services.json` with the new copy.
  - (If you ever regenerate the keystore, get the current fingerprint with: `keytool -list -v -keystore android/keystore/moment-release.jks`.)

- [x] **`TRUSTED_IMAGE_HOST_SUFFIX` is now set.** Found the real value in `backend/appsettings.Development.json` (`Cloudflare:PublicUrl` → `pub-750b02dac3184d00822e32cc8511df79.r2.dev`) and set it in both build types in `android/app/build.gradle.kts`. **Verify this still matches what your deployed backend actually uses** — that file is dev config, and if production's `Cloudflare__PublicUrl` env var points at a different host (e.g. a custom CDN domain), update the value in `build.gradle.kts` to match before shipping.

---

## Phase B — Backend: build, config, deploy verification

- [ ] **Confirm production environment variables are actually set** on whatever host you're using (Railway/Render/etc.) — I only edited code, I don't have access to your hosting dashboard:
  - `ConnectionStrings__DefaultConnection`
  - `Jwt__Key`, `Jwt__Issuer`, `Jwt__Audience`
  - `Cloudflare__AccountId`, `Cloudflare__AccessKeyId`, `Cloudflare__SecretAccessKey`, `Cloudflare__BucketName`, `Cloudflare__PublicUrl`
  - `FIREBASE_CREDENTIALS_BASE64` (production Firebase service account, base64-encoded)
  - `GoogleClientId`

- [ ] **Rebuild and redeploy the backend** now that the Dockerfile targets .NET 10 instead of 8 — the old Dockerfile would have failed to build at all.

- [ ] **Run the database migration manually before the new version goes live** (auto-migrate-on-startup is intentionally off — see `backend/DEPLOY.md`):
  ```
  cd backend
  dotnet tool restore
  dotnet ef database update --project Moment.Api.csproj
  ```

- [ ] **Wire the migration step into your actual deploy pipeline** so this isn't a manual step you have to remember every time — `backend/DEPLOY.md` has Railway/Render-specific suggestions (pre-deploy / release command).

- [ ] **Run a full local build and vulnerability scan** — I couldn't install the .NET SDK in my sandbox (network-restricted), so this hasn't been verified by a compiler yet:
  ```
  cd backend
  dotnet build
  dotnet list package --vulnerable --include-transitive
  ```

- [ ] **Double-check the `ForwardedHeadersOptions` fix matches your actual host.** I set it to trust the immediate proxy hop unconditionally (safe default for Railway/Render-style platforms with no public ingress bypassing them). If your setup is different, verify `context.Connection.RemoteIpAddress` reflects the real client IP in production logs, not your proxy's IP.

---

## Phase C — Android: build and real-device testing

- [ ] **Open the project in Android Studio and let Gradle sync.** I bumped the Android Gradle Plugin (8.2.2 → 8.13.0) and Gradle wrapper (8.5 → 8.13) to support `compileSdk`/`targetSdk` 36 — this is the one change I could not verify by actually compiling (no Android SDK in my sandbox). If Android Studio's Upgrade Assistant flags anything, resolve it here first before touching anything else.

- [ ] **Fix any build errors from the SDK/AGP bump.** Most likely spots: deprecated APIs touched by the API 35/36 behavior changes, background work/notification permission changes, and anything the Compose/Compose-BOM version might flag as incompatible with the newer AGP.

- [ ] **Build a signed release AAB** and confirm it actually produces a signed artifact:
  ```
  cd android
  ./gradlew bundleRelease
  ```
  (Or Android Studio: Build → Generate Signed App Bundle, pointing at `keystore/moment-release.jks`.)

- [ ] **Install the signed release build on a real device** (not just an emulator — release-only crashes and ProGuard/R8 issues won't show up in debug). Sideload the release APK/AAB or use `bundletool`.

- [ ] **Full manual QA pass on that release build**, checking off each flow:
  - [ ] Fresh install → Google Sign-In succeeds (this is the one most likely to break if the SHA-1 step above was skipped)
  - [ ] Username onboarding: mandatory, enforces the `a-z0-9_`, 4–20 char rule, rejects duplicates
  - [ ] Terms of Service / Privacy Policy acceptance is actually captured during onboarding (the backend has `TermsAcceptedAt`/`PrivacyAcceptedAt` fields for this — confirm the onboarding flow sets them; I did not verify this end-to-end)
  - [ ] Create pairing key, join with it from a second account/device, connection becomes active
  - [ ] Send a moment: camera capture, and gallery picker
  - [ ] Image editor (draw + text overlay) — confirm it no longer freezes/lags on a large photo, and "Continue" shows a spinner while saving
  - [ ] Moment applies to Home, Lock, and Both targets correctly
  - [ ] Recipient gets the "❤️ [name] left something on your screen" notification
  - [ ] Force-kill the app before an FCM push arrives, then reopen — confirm `GET /api/moments/pending` recovers it (pending-moment sync path)
  - [ ] Send the same moment notification twice (or simulate a duplicate FCM delivery) — confirm it does **not** re-apply or double-notify
  - [ ] Favorite / reaction flow, and that the partner gets notified
  - [ ] Widget: add to home screen, confirm it updates and doesn't block the UI thread
  - [ ] Settings → Report Partner: submit a report, confirm success state
  - [ ] Settings → Block Partner: confirm it unpairs immediately and that the blocked user genuinely cannot re-pair with you via a new invite
  - [ ] Settings → Unpair (normal, non-block path) still works
  - [ ] **Delete Account**: confirm the confirmation dialog appears, deletion actually completes, you're returned to Login, and you cannot log back in and see old data
  - [ ] Pause/resume moments ("Take Space")
  - [ ] Logout and log back in — session restores correctly
  - [ ] Airplane mode / no network: app fails gracefully, no crashes
  - [ ] Confirm Crashlytics is receiving events from this release build (trigger a test non-fatal if needed)

---

## Phase D — Google Play Console

- [ ] **Create the app listing**: title, short/full description, icon, feature graphic, phone screenshots (you'll need at least a few — screenshot the actual release build from the device testing above).

- [ ] **Content rating questionnaire** (IARC) — a few minutes, required for the app to appear publicly.

- [ ] **Target audience & content settings** — this app is 1:1 pairing between adults sharing private photos; make sure the age/audience declarations reflect that accurately (not a kids app).

- [ ] **Data Safety form** — must now be truthful given the fixes above: declare photo/image collection, personal info (email, display name), device identifiers (FCM token), and critically mark **"users can request data deletion"** as true, since that's now actually implemented. List:
  - Privacy Policy URL: `https://moment-app.com/privacy` (already linked in-app — confirm this page is live and current)
  - Account deletion URL: needs a **web page**, separate from the in-app flow, per Google's policy (a page describing how to delete the account, or a form to request it) — this doesn't exist yet as far as I can tell and needs to be created and linked in the Data Safety form.

- [ ] **App signing**: when you upload your first AAB, enroll in Play App Signing (Google will manage the actual signing key going forward, using your uploaded keystore as the "upload key" — this also gives you a recovery path if the upload key is ever compromised, though you should still keep the backup from Phase A).

- [ ] **Upload the signed AAB** from Phase C.

- [ ] **Since your account was created in 2020 and already has a published app, you are exempt from the 12-tester/14-day closed testing requirement** — you can go straight to production rollout once the above is complete. (Confirmed this from Google's policy in our earlier conversation — worth double-checking your account status in Play Console hasn't changed, but this should hold.)

- [ ] **Review before hitting publish**: re-read the Data Safety answers against what the app actually does one more time: Google rejects/suspends apps for mismatches between the form and real behavior more often than almost anything else.

---

## Known deferred items (not blockers, but don't forget them)

- [ ] **Per-moment "Report Moment" UI.** The backend endpoint and the Android repository method (`reportRepository.reportMoment`) both already work — only "Report Partner" (account-level) has a tap target in Settings so far. Worth adding a per-moment report action once you decide where it should live in the custom moment-timeline UI.
- [ ] **Certificate pinning.** `network_security_config.xml` is in place but intentionally not pinned yet (guessing wrong pins would hard-break all network traffic). Instructions for generating the correct pin safely are in the comments of that file, for whenever you want to add it.
- [ ] **Paywall/premium.** Confirmed `ui/paywall` is still empty — nothing to hide for this launch. When you do build it, it must use Google Play Billing, not any other payment method, or the app will be rejected.
- [ ] **Version bumping discipline going forward.** `versionCode`/`versionName` are still `1`/`"1.0.0"`. Every future Play Console upload needs a strictly higher `versionCode` — bump it as part of your release process from here on.
