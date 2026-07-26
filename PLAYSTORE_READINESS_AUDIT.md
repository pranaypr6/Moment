# Moment — Play Store Readiness Audit
Date: 2026-07-26

Scope: full read-only review of `backend/` (ASP.NET Core / PostgreSQL) and `android/` (Kotlin / Compose), plus current Google Play policy requirements. Nothing was changed — this is a punch list.

## Verdict

Not ready to submit yet. Two of the findings below (fake account deletion, missing report/block) are Play policy blockers for an app in this category, and one (no release signing config) means a release build literally cannot be produced yet. Everything else is fixable in a focused sprint — the core architecture (auth, data model, upload pipeline, worker reliability) is genuinely solid.

---

## Must-fix before submission (Blockers)

### 1. "Delete Account" doesn't delete anything — Android
`ui/navigation/NavGraph.kt:125-128` — the button just clears the nav stack and returns to Login:
```kotlin
onNavigateToDeleteAccount = {
    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
}
```
There is no delete-account call anywhere in `AuthApi.kt`. The user is shown a "delete" action that does nothing — their account, photos, and moment history all remain. This is a direct violation of Play's User Data policy (in-app account deletion is mandatory for any app with account creation) and, worse, it's actively misleading.

### 2. Backend account deletion silently fails and never cleans up storage
`backend/Services/AuthService.cs:181-200` (`DeleteAccountAsync`):
```csharp
var rel = await _context.Relationships
    .FirstOrDefaultAsync(r => r.Partner1Id == userId || r.Partner2Id == userId);
if (rel != null) _context.Relationships.Remove(rel);
_context.Users.Remove(user);
try { await _context.SaveChangesAsync(); }
catch (DbUpdateException) { /* swallowed */ }
```
Only grabs **one** relationship row (a re-paired user has more — FKs are `Restrict`, so deletion throws and is silently caught, meaning the API returns success while nothing was deleted). It also never deletes the user's images/thumbnails from Cloudflare R2 — media is orphaned forever. Google's account-deletion requirement is explicit: you must delete the account and *all* associated data, and you can't fake it with a no-op.

**Fix together**: build a real delete flow — Android calls a real endpoint; backend deletes all relationships/devices/invites/reports for the user, deletes all R2 objects (image + thumbnail + profile picture), then removes the user, and never swallows the exception (log + return failure instead of a false 200).

### 3. Report Moment / Report User / Block User — not implemented in the app
Your own `prd.md` (and Google Play's User Generated Content policy, since this app sends images+notes between users) requires these. The backend already has the `Report` model and migration for it, but there is no `ReportController`/service method that creates one, and the Android app has zero UI for it — `SpaceSettingsScreen.kt` only offers "Unpair." For an app in this category this is a near-certain rejection risk.

### 4. No release signing config
`android/app/build.gradle.kts` `buildTypes.release` has no `signingConfig`. Right now `bundleRelease` can't produce an uploadable, properly signed AAB. Needs a real keystore + a `signingConfig` wired from local `keystore.properties` (gitignored) or CI secrets — never commit the keystore or passwords.

### 5. Dockerfile can't build the backend as currently configured
`Moment.Api.csproj` targets `net10.0`; `Dockerfile` uses `sdk:8.0`/`aspnet:8.0` base images. A .NET 8 SDK image cannot restore/publish a net10.0 project — this will fail your deploy, not the Play submission, but it blocks having a working backend to ship against. Bump the Dockerfile images to `10.0` (and add a `global.json` to pin the SDK so this can't silently drift again).

### 6. `targetSdk`/`compileSdk` = 34 is stale for 2026
Per Google Play's current policy: **new apps/updates need to target API 36 (Android 16) starting Aug 31, 2026** (extension available to Nov 1, 2026 if requested), and **existing apps need API 35 (Android 15)** to stay visible to new users on newer OS versions. You're at 34. Since you're submitting fresh, budget time to bump `compileSdk`/`targetSdk` and fix whatever breaks (behavior changes tend to hit background work, notifications, and permissions the most — worth a full regression pass after bumping). [Target API requirements](https://developer.android.com/google/play/requirements/target-sdk) · [2026 details](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en)

### 7. `versionCode`/`versionName` frozen at `1` / `"1.0"`
Despite ~20+ feature commits. If this was ever uploaded as a draft, the next upload will fail outright (`versionCode` must strictly increase). Set up real versioning before your first upload.

---

## High priority (fix before or very shortly after launch)

**Backend**
- **Moment/invite rate limits are configured but never enforced.** `appsettings.json` defines `MomentLimits` (5/hour, 20/day) but no code ever reads it — `MomentController.Create` and `RelationshipController.CreatePairingKey` have zero rate limiting. Right now one account can spam unlimited wallpaper moments and invite codes. `PresenceService` already has the DB-backed counting pattern to copy.
- **No server-side validation on almost every write DTO.** Username format (your own `a-z0-9_`, 4–20 char rule), the 250-char note limit, display name length — none are enforced server-side (`CreateProfileRequest`, `CreateMomentRequest`, `UpdateProfileRequest` are essentially unvalidated). Client-side checks are trivially bypassed.
- **`ImageUrl`/`ThumbnailUrl` on a moment are client-supplied and not checked against your own storage domain.** A sender could point a moment at an arbitrary attacker-controlled URL that gets pushed to their partner as a trusted notification. Validate it starts with your configured R2 public URL prefix.
- **Debug logging leaks token info on the login path.** `AuthController.cs` has leftover `Console.WriteLine` calls logging `IdToken` length/prefix on every Google login request — unauthenticated, hot path, sensitive. Strip it and move logging to `ILogger` generally (23 `Console.WriteLine` call sites total).

**Android**
- **FCM service trusts unsigned payload data.** `MomentFirebaseMessagingService` is exported (standard for FCM) but doesn't validate that `imageUrl` in the push payload actually points at your CDN/R2 host before downloading and applying it as wallpaper. Add a host allow-list check in `WallpaperWorker` before it downloads anything from a push payload.
- **No release build has actually been produced/tested.** `proguard-rules.pro` is nearly empty (though DTOs are `@Keep`-annotated, which helps). Combined with the missing signing config, there's no evidence a signed release AAB has ever been built and smoke-tested end-to-end (login → pair → send moment → wallpaper apply). Do this before submission — it's the single most common source of "works in debug, breaks in production."

---

## Medium priority

- **Backend sits behind a proxy without `KnownProxies`/`KnownNetworks` configured** for `ForwardedHeadersOptions` — your IP-based auth rate limiter likely sees the proxy's IP for every request, not the real client, collapsing everyone into one shared rate-limit bucket. Verify `RemoteIpAddress` behavior in your actual hosting environment.
- **No documented migration-on-deploy step.** Auto-migrate-on-startup was deliberately disabled (correctly — it's risky with multiple instances) but there's nothing in its place; migrations currently rely on someone remembering to run `dotnet ef database update` manually. Add an explicit CI/deploy step.
- **`ImageEditorScreen` decodes/saves bitmaps synchronously on the main thread** (inside a plain Compose `remember` block and a direct button `onClick`) — real ANR risk on large camera photos, inconsistent with how `SendMomentViewModel` correctly does the same work on `Dispatchers.IO`.
- **`WallpaperWorker` doesn't check if a moment was already applied before reapplying it.** FCM is at-least-once delivery, so duplicate pushes are normal; a redundant download+apply+notification can fire. `MomentDao.getMomentById()` already exists to guard this — it's just not called at the top of `doWork()`.
- **No certificate pinning / `network_security_config.xml`.** Not a hard Play requirement, but worth adding given the app handles intimate photos between partners.
- **Unused `RECEIVE_BOOT_COMPLETED` permission** declared with no boot receiver anywhere in the code — remove it, it's an unnecessary permission-declaration flag on Play Console for zero benefit.

## Low priority / cleanup

- `Report` DB model exists but has no controller wired up yet (needed for the Report Moment/User fix above anyway).
- Run `dotnet list package --vulnerable` before shipping — versions look current at a glance but weren't cross-checked against a live CVE feed.
- `GlobalScope.launch` used in the widget's presence-action callback — minor unstructured-concurrency smell, low practical impact.
- Debug ngrok URL / `ngrok-skip-browser-warning` header is correctly confined to debug builds (verified — release build type overrides `BASE_URL` correctly), just tidy it behind `BuildConfig.DEBUG`.

---

## What's already solid (no action needed)

- **AuthZ/IDOR**: every backend endpoint derives identity from the validated JWT claim, never a client-supplied ID; relationship/moment access is consistently ownership-scoped.
- **No SQL injection surface** — 100% EF Core LINQ, no raw/interpolated SQL anywhere.
- **Upload pipeline**: pre-signed URLs, server-generated GUID object keys (no path traversal), content-type allow-list + size cap + magic-byte re-validation after upload.
- **CORS**: explicit origin allow-list, no wildcard+credentials misconfiguration.
- **JWT lifecycle**: no hardcoded fallback signing key, short-lived access token, hashed refresh token stored server-side.
- **DB indexing**: matches the PRD's own recommendation (Sender/Receiver/CreatedAt/Status indexes present).
- **Token storage on Android**: genuinely uses `EncryptedSharedPreferences` with a hard-fail (no insecure fallback) if the Keystore is unavailable.
- **OkHttp logging is hardcoded to `Level.NONE`** — no token/body leakage into logcat in any build variant.
- **No secrets committed to git** — `google-services.json`, `appsettings.Development.json`, Firebase admin key, keystores are all correctly gitignored and confirmed absent from git history.
- **`WallpaperWorker` bitmap handling is genuinely hardened** — capped decode size, `OutOfMemoryError` caught, retry/backoff, foreground service type set correctly for API 29+.
- **Wallpaper-change notification is implemented** — satisfies the "never change wallpapers silently" requirement.
- **Privacy Policy / Terms links are live and correctly wired** in-app.

---

## Google Play policy notes relevant to this launch

- **Target API level**: existing apps need API 35+ to stay visible to new users on newer OS; **new apps/updates need API 36 starting Aug 31, 2026** (extension to Nov 1, 2026 available on request). [Source](https://developer.android.com/google/play/requirements/target-sdk)
- **Account deletion**: if the app supports account creation (it does, via Google Sign-In), you need both an in-app path to delete the account+data *and* a web URL for deletion requests that must be listed in Play Console's Data Safety form and match your privacy policy. Deletion must be real and complete — you cannot just deactivate/freeze the account. [Source](https://support.google.com/googleplay/android-developer/answer/13327111?hl=en)
- **Data Safety form**: you'll need to declare what data is collected (photos, notes, email, device info, FCM tokens) across the required categories, and the "can users request deletion" answer must be true and accurate once #1/#2 above are fixed.

Sources:
- [Target API level requirements for Google Play apps](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en)
- [Meet Google Play's target API level requirement — Android Developers](https://developer.android.com/google/play/requirements/target-sdk)
- [Understanding Google Play's app account deletion requirements](https://support.google.com/googleplay/android-developer/answer/13327111?hl=en)
- [About the Data Safety Form and Account Deletion](https://support.google.com/googleplay/android-developer/community-guide/246344978/about-the-data-safety-form-and-account-deletion?hl=en)

---

## Suggested order of work

1. Fix Dockerfile SDK mismatch (unblocks deploying a working backend at all).
2. Build the real account-deletion flow end to end (Android button → API → DB + R2 cleanup, no swallowed exceptions).
3. Add Report Moment / Report User / Block User (backend endpoint + Android UI).
4. Add release signing config, bump `compileSdk`/`targetSdk` to current requirement, bump `versionCode`/`versionName`, produce and manually test a signed release build through the full core flow.
5. Wire up the existing rate-limit config for moments/invites; add DTO validation attributes; validate moment image URLs against your own domain; strip debug token logging.
6. Fix the `ImageEditorScreen` main-thread bitmap work and the missing dedup check in `WallpaperWorker`.
7. Everything in Medium/Low as time allows — none of it blocks submission.
