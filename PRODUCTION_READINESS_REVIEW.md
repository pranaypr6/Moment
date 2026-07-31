# Moment — Final Production Readiness Review

**Date:** 2026-07-29
**Reviewed as:** Principal Engineer / Senior Android / Senior .NET Architect / Security Engineer / QA Lead / DevOps / Play Store reviewer
**Scope:** Full Android app, ASP.NET Core (.NET 10) backend, database, APIs, auth, widgets, notifications, camera, Cloudflare R2, Firebase Auth/FCM, storage, UI/UX, architecture, performance, security, scalability, privacy, release config.
**Method:** Four independent deep-read audits (Android, Backend, Security/OWASP, UX+Play Store), cross-verified against actual source and git history — not taken on faith from prior session notes or existing audit docs in this repo.

Assume the load-bearing assumption stated in the brief: 100,000 real users, day one.

---

## VERDICT

# 🔴 NOT READY FOR PRODUCTION

Two **CRITICAL** security findings alone are disqualifying regardless of anything else in this document: real user credentials/PII are currently sitting in a committed file, and the original JWT signing secret is permanently visible in this repo's git history. Both must be resolved before this app touches real users, independent of every other finding below.

The good news, stated plainly so it isn't lost under the bad news: the authorization architecture (IDOR protection), token storage on Android, TLS configuration, account deletion, and file-upload validation are all **genuinely well-built** — better than most first-launch apps. This is not a rewrite. It's a focused punch list.

---

## Scores

| # | Category | Score /100 | Why |
|---|---|---|---|
| 1 | **Production Readiness** | **38** | Two CRITICAL live-security findings, several HIGH scalability/security gaps, one crash-risk blocker, and dead/duplicate features block launch today despite a solid foundation. |
| 2 | UI | 72 | Genuinely distinctive custom work (aurora header, particle effects, hand-drawn camera UI) pulled down by inconsistent chrome (flat black editor screen), duplicate settings UI, unstyled camera-permission fallback. |
| 3 | UX | 65 | Clean onboarding/pairing flow; undercut by a dead consent-capture feature, a stripped duplicate settings screen, and several accessibility gaps. |
| 4 | Performance | 60 | Good bones (off-main-thread image decode, correct WorkManager retry/backoff in most workers) undone by an unreleased camera resource, an unindexed hot-path DB query, and an unconstrained/unbounded retry worker. |
| 5 | **Security** | **25** | Dominated by two CRITICAL findings (live PII/token exposure in a committed file; historically hardcoded JWT secret + DB password permanently in git history) despite strong IDOR protection, encrypted token storage, and correct TLS config elsewhere. One live leak caps this score regardless of what else is right. |
| 6 | Architecture | 70 | Clean repository/Hilt/MVVM separation and correct WorkManager de-dup design; docked for two ViewModels/screens independently reimplementing the same relationship-mutation logic and a fully dead invite/deep-link feature. |
| 7 | Scalability | 55 | Solid DB pagination and connection-scoping; an unindexed column hit on every token refresh and two unthrottled high-frequency endpoints are real "falls over under load" risks at 100k users, not theoretical ones. |
| 8 | Maintainability | 58 | Real duplicate logic across two settings screens with already-diverging behavior, dead code paths, and one misleading stale code comment on a security-relevant value. |
| 9 | Code Quality | 60 | No systemic smells beyond the duplication/dead-code items; disciplined, well-commented error handling in most of the backend. |
| 10 | Play Store Readiness | 55 | Signing, manifest, icons, and ProGuard rules are genuinely solid and previously-flagged issues are verified fixed; blocked by a dead consent-capture feature, a Data-Safety-form/actual-behavior mismatch risk, and an inconsistent splash screen across supported OS versions. |

---

## CRITICAL — Must fix before anything else

### C-1. Real user PII and Google Sign-In tokens are committed and currently in the repo
**File:** `log.txt` (repo root) — confirmed tracked in git (`git ls-files`), confirmed on disk at 8.5MB, confirmed to contain JWT-structured content (`eyJ...` base64 segments). This is a raw logcat capture from a session where `HttpLoggingInterceptor` was running at `BODY` level, containing full request/response bodies for the Google Sign-In exchange — real emails, real display names, real Google profile photo URLs, and real (short-lived but real) signed ID tokens.

- **Why this matters:** this is not a theoretical vulnerability — it is real people's data, already exposed, right now, in this codebase's history.
- **Action:** purge `log.txt` from git history entirely (`git filter-repo` or BFG — deleting the file in a new commit is not sufficient, it must be removed from history), force-push, and have every collaborator re-clone. Delete the file from the working tree now regardless. Confirm whether the repository has ever been public at any point and, if so, treat the exposed emails/tokens as compromised (tokens are short-lived and likely already expired, but the PII is not).

### C-2. The original JWT signing key and database password were hardcoded and committed
**File:** `backend/appsettings.json`, initial commit `4245269` ("chore: initial fresh commit"):
```
"Jwt": { "Key": "moment_secret_key_should_be_long_enough_and_secure" }
"ConnectionStrings": { "DefaultConnection": "...Password=postgres" }
```
Fixed in a later commit (`cf61840`) to environment-variable substitution — but the literal original values are permanently visible in git history to anyone with repo access.

- **Why this matters:** if the deployed production `JWT_SECRET_KEY` was ever actually set to this exact literal string during the window before the fix, anyone who reads this repo's history can mint a validly-signed JWT for **any user ID** and fully impersonate any account — this is a symmetric (HS256) signing key, and the server trusts it completely.
- **Action:** confirm out-of-band that production's `JWT_SECRET_KEY` and DB password were rotated to values that were never the literal strings above. If unconfirmed, rotate both immediately — this will invalidate all active sessions, which is an acceptable one-time cost against the alternative.

---

## HIGH — Must fix before general availability at scale

### H-1. `Users.RefreshToken` / `PreviousRefreshToken` have no database index
Every token refresh (roughly every 15 minutes per active user, given the access-token TTL) runs `WHERE RefreshToken == hash OR PreviousRefreshToken == hash` against a table with **no index on either column**. At 100,000 users this becomes a continuous full-table scan on the single hottest query path in the app. This will show up as production latency almost immediately, not at some distant future scale.
*Fix:* add indexes on both columns (a migration, not a code change).

### H-2. No rate limiting on presence-signal or moment-creation endpoints
`POST /api/v1/presence/signal` and `POST /api/moments` have **zero** `[EnableRateLimiting]` coverage. The only throttle is an in-service counted check that reads-then-writes with no atomicity (a race, not a hard cap), and neither endpoint has any per-second ceiling at all. An attacker can burst either endpoint far faster than the hourly/daily counters can react, hammering the database.
*Fix:* apply the existing `EmotionalLimiter`/a dedicated policy to both endpoints.

### H-3. Global (non-partitioned) rate limit on the pairing endpoint
`RelationshipController.Join`'s `JoinLimiter` (5 requests/minute) is a single shared bucket for the **entire application**, not per-user or per-IP. Any single authenticated user can exhaust it, causing every other user in the app to get 429s on pairing for a full minute — a trivial, repeatable denial-of-service against a core feature.
*Fix:* partition this limiter by user ID or IP like every other limiter in the app already correctly does.

### H-4. Profile picture URLs are never validated server-side
`CreateMomentRequest.ImageUrl` is strictly validated against the app's own storage domain before being trusted — but `ProfilePictureUrl` on both profile-creation and profile-update requests has **no equivalent check**. A user can set their profile picture to any arbitrary URL, and the server forwards it directly to their partner's client as trusted content. This is precisely the "server sends an untrusted URL the client blindly trusts" risk the Android side already defends against for moment images specifically — profile pictures are the one gap.
*Fix:* apply the same host-prefix validation used for moment images to profile picture URLs.

### H-5. No session revoke / logout endpoint, and refresh-token theft is invisible
There is no `/logout` or `/revoke` route anywhere in the API. Combined with the refresh-token grace-window design (a deliberate, documented, and reasonable fix for a real mobile-network bug), a stolen refresh token used concurrently with the legitimate one is **not detected or cut off** — both sides can keep refreshing indefinitely as long as each does so within about 2 minutes of the other. This is bounded (requires the token to already be stolen) but there is currently no way for a user or admin to kill a session early if compromise is suspected.
*Fix:* add an explicit revoke/logout endpoint that clears both token slots server-side; consider tightening the grace window if abuse is observed.

### H-6. Camera hardware is never released when leaving the capture screen
**Android.** `CameraCaptureScreen.kt` binds the camera to the Activity's lifecycle with no `DisposableEffect`/`onDispose` unbinding it. After capturing a photo and navigating to the image editor, the camera pipeline keeps running in the background — draining battery and keeping the Android 12+ "camera in use" indicator lit — until the app is backgrounded or the capture screen is re-entered.
*Fix:* unbind the camera provider in `onDispose` when the composable leaves composition.

### H-7. R2 storage bucket is fully public with no expiry or revocation, serving the app's most sensitive content
Filenames are unguessable (GUID-based), which is the primary and currently only mitigation — but every moment photo and thumbnail two partners exchange is permanently and unconditionally public to anyone who ever obtains the URL, with no auth check, no expiry, and no revocation on individual moment deletion. The URL also travels through the FCM data payload (through Google's infrastructure) and is cached in local device logs.
*Fix:* at minimum, make this an explicit, documented product decision rather than an implicit one; consider short-lived signed URLs for full-resolution originals.

---

## Functionality — completeness check

- **Dead feature, fully non-functional end-to-end:** the invite-via-link / Play Store referral flow. The Android side saves a pending invite code from the install referrer, but nothing anywhere in the app ever reads it back to auto-join a pairing. The `Screen.Main` route's `inviteCode` argument and `MainScreen`'s corresponding parameter are accepted and never used. A user who installs via a referral link gets nothing — the feature silently does not work.
- **Dead schema, no real implementation:** `TermsAcceptedAt`/`PrivacyAcceptedAt` exist on the backend `User` model across several migrations, but nothing anywhere — backend or Android — ever writes to them. There is no actual consent-capture UI at signup despite this looking, from the schema alone, like a compliance feature that exists.
- **Duplicate, behaviorally-diverging implementations of the same feature:** the "Us" tab's inline relationship-settings panel and the separate gear-icon `SpaceSettingsScreen` both implement unpair/block/report independently, with two separate ViewModels doing the same relationship mutations. They have already drifted: the gear-icon screen's plain "unpair" navigates back immediately without waiting for the API call to succeed (silent failure risk), while the Us-tab version does not force navigation. The gear-icon screen is also missing the anniversary-date control, vibe display, and loading states the Us-tab version has.
- **Crash risk on a real user action, not an edge case:** `ImageEditorScreen`'s save-and-continue path has no try/catch around a coroutine that can throw (an unclosed `FileOutputStream` not wrapped in `.use {}`), launched from a UI-triggered coroutine scope with no supervisor — an exception here crashes the app and leaves the "saving" spinner stuck if it somehow survives.
- No other incomplete/placeholder/TODO-marked logic was found in either codebase beyond the items above and the notes in the sections that follow. Report/Block, account deletion, and the wallpaper-apply pipeline are all real, complete implementations — verified by reading the actual logic, not the claims of prior audit documents in this repo.

---

## Android — engineering findings

- **Unstructured coroutines beyond the two previously-known instances:** the widget's post-tap "reset to idle" delay, `MomentFirebaseMessagingService.onNewToken`, and its presence/vibe signal handlers all use detached `CoroutineScope(...).launch {}` blocks with no process-death survival. If the process dies mid-flight (very plausible for FCM handling, which is not guaranteed foreground time), a device token silently never registers, or a widget gets stuck showing "Sending..." forever. These should move onto WorkManager one-time work, the same pattern already correctly used for wallpaper/moment sending.
- `SendPresenceWorker` has no network constraint and no bounded retry-attempt ceiling (every other worker in the app correctly caps retries at 3–5 attempts). A persistently failing presence-send will retry indefinitely on WorkManager's backoff schedule, waking the device periodically forever.
- The widget's periodic 6-hour refresh is a reasonable cadence; a vestigial, functionally-dead 24-hour `updatePeriodMillis` also remains declared in the widget's XML info file (Glance manages its own update path — this legacy field does nothing but is confusing to find).
- Logout does not clear the cached profile (display name, profile picture URL) from local storage — a real data-hygiene gap on a shared/reused device for a couples app, inconsistent with account deletion, which does clear everything correctly.
- Room's schema uses `fallbackToDestructiveMigration()` — acceptable for launch since the server is the source of truth for the scrapbook, but any in-flight offline-outbox moments will be silently dropped on a future schema-bumping app update. Worth a real migration path before it's needed.
- Debug and release builds point at the same production backend — there is no separate staging environment, meaning any local development testing hits production data directly.
- A stale code comment in `build.gradle.kts` describes a security-relevant build config value (the trusted-image-host allow-list) as an unset placeholder when it is, in fact, correctly populated and active — the security control itself is fine, but the comment will mislead the next person who reads it into thinking there's a gap that doesn't exist.

---

## Backend — engineering findings

- Ownership/IDOR checks are consistently and correctly applied across every controller reviewed (relationship, moment, report, presence, device) — this is the single strongest part of the codebase and should be preserved as the standard for anything added later.
- `RelationshipController`'s pairing-key creation, space-name/cover/anniversary/pause update endpoints have no application-level exception logging before falling through to the generic handler — not a leak, but a real production-debuggability gap (a failure there is invisible in logs beyond framework defaults).
- `AuthController.IsUsernameAvailable` is public, unauthenticated, and completely unthrottled — unlimited username enumeration and query volume from any anonymous caller.
- `DeviceService` reassigns an FCM token's owning user based solely on the token value with no prior-ownership check — low practical risk since tokens aren't guessable, but an intentional trust-the-client design worth hardening if device-registration abuse is ever observed.
- Missing composite indexes on `Invites(SenderUserId, CreatedAt)` and `Moments(CreatorUserId, CreatedAt)` — both are hit by rate-limit counting queries on hot paths (invite creation, moment sending); only single-column indexes exist today.
- No raw/concatenated SQL anywhere — no SQL injection surface found. No blocking synchronous calls in async paths. DbContext lifetime and the R2 client's singleton registration are both correctly scoped.
- Cursor-based scrapbook pagination is a textbook-correct keyset implementation with no drift/duplicate/skip risk.
- Presigned-upload validation is genuinely strong: server-generated (non-client-supplied) object keys, content-type enforcement baked into the signature, and a real post-upload magic-byte check on the actual uploaded bytes (not just client-declared metadata) — better than most MVP-stage backends. The one gap: actual object size is never re-verified after upload via a HEAD request, so a declared-small/actually-large upload isn't caught.

---

## Security — OWASP-mapped summary

Beyond the two CRITICAL findings above:

- **OWASP Mobile M6 (Privacy):** notification content (presence signals, vibe updates, "X left you something") is shown on the lock screen by default — no explicit `VISIBILITY_PRIVATE` + redacted public fallback is set on any of the four notification builders. For an app whose entire premise is intimate, private signals between two people, this is a real exposure to anyone with brief physical access to a locked phone. Confirm this is a deliberate product decision, not an oversight, before launch.
- **OWASP Mobile M6/M9:** the home-screen widget shows both partners' photos, mood/vibe status, and a days-together counter with no gating — a passive, always-visible disclosure on any unlocked or shared device. Same call: confirm intentional.
- Token storage on Android is genuinely correct: real `EncryptedSharedPreferences` (AES-256), with a hard failure rather than any plaintext fallback.
- Google Sign-In token verification server-side is real cryptographic signature verification against Google's live public keys with audience checking — not a bare decode-and-trust.
- TLS/cleartext configuration is correct throughout: cleartext disabled app-wide, trust anchors restricted to system CAs, no custom TrustManager/HostnameVerifier bypass anywhere in the codebase. Certificate pinning is deliberately deferred with a documented, reasonable rationale — an accepted residual risk, not a defect.
- Every `PendingIntent` in the codebase correctly uses `FLAG_IMMUTABLE`.
- The FCM payload trust boundary (wallpaper images must be HTTPS and match the app's own storage host suffix) is real and active, not a stub.
- A Google Sign-In nonce is generated client-side but never verified server-side — a defense-in-depth gap of low practical severity given TLS already covers the realistic interception scenario.

---

## Performance

- Image decoding is correctly moved off the main thread with proper downsampling in the editor flow — this closes what was previously a real ANR risk.
- Most background workers (wallpaper apply, moment send) have correct WorkManager constraints, bounded retries, and explicit `OutOfMemoryError` handling.
- The Compose BOM and Hilt versions in use are roughly two years old relative to today's date, running against a much newer `compileSdk`/`targetSdk` (36) — worth a compatibility pass; not a launch blocker by itself, but a real source of latent, hard-to-diagnose recomposition/animation bugs if left indefinitely.
- The unindexed refresh-token query (H-1) and the unconstrained presence-worker retries are the two performance findings most likely to actually bite in production, not the recomposition-level nitpicks.

---

## UX

- Onboarding and the pairing flow (create-or-join a relationship via a pairing key) are clean, low-friction, and well-designed — a genuine strength.
- The custom visual work in the main "Us" screen (ambient header animation, particle effects) and the camera capture UI's hand-drawn aesthetic show real design investment and read as distinctive rather than generic.
- The duplicate settings screens (Us tab vs. gear icon) directly undercut the "premium" feel the rest of the app earns — users notice when the same feature looks and behaves differently in two places, and one of the two paths is measurably less finished.
- The camera permission-denied state is a bare, unstyled line of text with no recovery action, in an app that otherwise consistently designs its empty/error states with real icons and copy.
- Several icon-only controls (image-editor color swatches, some quick-reaction icons) have no accessible label and/or sit at or under common minimum touch-target guidance.

---

## Play Store Readiness

**Genuinely solid, verified correct — not just claimed:**
- `targetSdk`/`compileSdk` 36 — already ahead of the relevant upcoming Play submission deadline.
- Release signing fails safe: a release build with no keystore present will hard-fail rather than silently falling back to debug signing, and a real keystore currently exists.
- Adaptive icon resources are real, layered, not a flat fallback.
- ProGuard/R8 rules are targeted correctly at the actual reflection-dependent code (Retrofit/Gson/Room/coroutines) in use.
- `allowBackup="false"` correctly blocks every backup pathway (cloud, ADB, device transfer) app-wide, making the otherwise-too-permissive backup/data-extraction rule files currently inert (though they should still be tightened now, while it costs nothing, in case that flag ever changes).
- Account deletion is real, transactional, and cleans up both the database and cloud storage — previously flagged issues here are confirmed fixed.
- Report/Block are real, implemented features on both client and server — previously flagged as missing, confirmed now present.

**Needs work before submission:**
- No actual Terms/Privacy consent is ever captured at signup despite the schema existing for it (see Functionality section) — this needs either a real consent gesture or removal of the misleading unused fields.
- The privacy policy text does not currently disclose Crashlytics or Analytics data collection, nor call out presence/emotional-signal data as its own category — this needs to match whatever gets declared in the Play Console Data Safety form, or the mismatch itself becomes a policy violation.
- The splash screen is only correctly branded on Android 12+ (API 31+); devices on API 26–30, which are within this app's own supported range, get an unstyled system-default flash on cold start.

---

## Code Quality

- The most significant maintainability risk in the codebase is the duplicate settings-screen/ViewModel pair described above — it has already caused a real behavioral bug (fire-and-forget navigation on unpair in one copy but not the other) and will continue to drift every time one copy is touched and the other isn't.
- The dead invite-code/deep-link feature and the dead consent-capture schema are both examples of features that look finished from the data model alone but have no working implementation — a trap for future contributors (or reviewers) who assume schema presence means feature presence.
- Error handling, logging discipline, and code comments (many of which document the exact reasoning behind non-obvious past bug fixes) are genuinely good practice throughout most of the backend and are worth preserving as the house style.

---

## Full Blocking Issue List (must fix before release)

1. Purge `log.txt` from git history entirely; treat any exposed tokens/PII as already compromised.
2. Confirm (and rotate if unconfirmed) that production's JWT signing key and database password were never the literal values hardcoded in the initial commit, now permanently visible in public git history.
3. Add a database index on `Users.RefreshToken` and `Users.PreviousRefreshToken`.
4. Add rate limiting to `POST /api/v1/presence/signal` and `POST /api/moments`.
5. Partition the pairing (`JoinLimiter`) rate limit by user/IP instead of a single global bucket.
6. Add server-side host/domain validation for profile picture URLs, matching the existing moment-image validation.
7. Add a session revoke/logout endpoint.
8. Fix the camera not being released when navigating away from the capture screen.
9. Wrap `ImageEditorScreen`'s save path in proper exception handling and safe file-stream closing.
10. Implement real Terms/Privacy consent capture at signup, or remove the unused schema fields so they stop implying a feature that doesn't exist.
11. Reconcile the privacy policy text and Play Console Data Safety form with actual data collection (Crashlytics, Analytics, presence/vibe signals) before submission.
12. Decide and document whether R2's fully-public, non-expiring photo URLs are an accepted risk for this app's private-photo content model, or move to signed URLs.

**Strongly recommended, not hard-blocking:** consolidate the duplicate settings screens/ViewModels; fix the unstyled splash screen on API 26–30; add a styled camera-permission-denied recovery state; confirm lock-screen notification visibility and widget content exposure are deliberate product decisions.
