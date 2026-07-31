# Moment — Fast-Follow Backlog

Everything intentionally deferred past this launch, consolidated in one place instead of scattered across the audit reports. Nothing here blocks shipping the current build. Pulled from `PRODUCTION_READINESS_REVIEW_V3.md` plus items decided in conversation since.

---

## Features
- **Delete a moment** (decided 2026-07-30): either partner can delete a moment, removing it for both and cleaning up the R2 file — the simplest of three options discussed, deferred specifically to not disturb the release build currently being finalized. Backend needs a `DELETE /api/moments/{id}` endpoint (copy the ownership-check shape already used by `PUT /moments/{id}/favorite`), null out `Relationship.CoverMomentId` if the deleted moment is the current cover (same pattern `DeleteAccountAsync` already uses), and call the already-existing `StorageService.DeleteFileAsync`. Android side needs a swipe or long-press affordance on `MomentsScreen.kt` plus a confirm dialog.

## Watch item — confirm in real usage, not just code review
- **Widget update reliability.** This was reported multiple times this session (vibe change, day count, profile picture) and is now fixed at the code level: `AuthRepositoryImpl.updateVibe()`/`updateProfile()` and `RelationshipRepositoryImpl.updateAnniversary()` all correctly call `RelationshipWidget.forceUpdate(context)`, and the wiring (Hilt entry point, widget receiver) was independently re-verified twice, not just claimed. It is not instant — `forceUpdate()` runs on a background `CoroutineScope(Dispatchers.IO)`, so there's a small, normal delay, not a true zero-latency update — but it should now reliably land within a second or two of any change, every time, not "until an unrelated redraw." Given how many rounds this took to actually pin down, it's worth explicitly re-confirming under real day-to-day use post-launch rather than assuming the fix holds forever unverified. If it ever regresses, the first thing to check is whether a recent change reintroduced a code path that mutates cached profile/relationship data without calling `forceUpdate()` afterward.

## Performance / reliability (MEDIUM)
- Widget rebuilds a fresh Coil `ImageLoader` on every repaint (`RelationshipWidget.getBitmap()`) instead of reusing the app's shared singleton — redownloads both profile photos from scratch on every widget update. `WallpaperWorker.kt` has a smaller instance of the same pattern.
- Widget's "sending..." action-confirmation state is reverted via a `GlobalScope` coroutine with a 1200ms delay — if the process dies in that window, the widget can get stuck showing "sending" indefinitely until the next tap.
- In-app "send a reaction" (Moments screen) has no retry/offline handling and silently swallows non-429 failures — and separately, even a *successful* send shows no confirmation toast at all (`MomentsScreen.kt:340` only ever gates the toast on the rate-limit-error message). The identical action from the widget goes through WorkManager with real retry — two implementations of the same feature, one reliable, one not.
- Small check-then-act race in moment hourly/daily rate limiting (`MomentService.cs`) — bounded in practice by `BurstLimiter` already sitting in front of it.
- `DeviceController.RegisterDevice` has no rate limiting, unlike every other mutating endpoint — amplifies the existing device/FCM-token-reassignment gap below.
- No burst-rate-limit on pairing-key creation (only the daily cap).
- Missing max-length validation on `RegisterDeviceRequest.FcmToken` and `JoinRelationshipRequest.PairingKey` — an oversized value can trigger a raw Postgres error (uncaught 500) instead of a clean 400.

## Security / architecture (MEDIUM, non-blocking)
- Moment photos/notes stored unencrypted in the local Room DB (session tokens are correctly in `EncryptedSharedPreferences`; the app's actual sensitive content isn't). Requires root/physical device access to exploit, not remote.
- `androidx.security:security-crypto:1.1.0-alpha06` — long-lived pre-1.0 alpha dependency backing the one encrypted store.
- Device/FCM token reassignment (`DeviceService.RegisterDeviceAsync`) has no ownership check — a token already tied to one user gets silently reassigned to whoever registers it next.
- Single refresh-token-per-user architecture — logging in on a second device silently invalidates the first device's session (no per-device sessions despite a `Device` table existing). May be an intentional design choice, not a bug — worth a deliberate decision either way.
- Google Sign-In nonce generated client-side, never validated server-side (the DTO doesn't even transmit it currently, so this can't be fixed without an API change).
- `IsUsernameAvailable` endpoint has no auth and no rate limit — anonymous username enumeration is possible.
- No `UseHsts()` in the backend pipeline.

## UX / polish (MEDIUM/LOW)
- Image-editor color swatches (`ImageEditorScreen.kt`) are both unlabeled for accessibility and below Android's 48dp minimum touch-target size (currently 32dp).
- Login screen text contrast fails WCAG AA even at the large-text floor (~2.3:1 and ~1.8:1 measured on two labels).
- `SpaceSettingsScreen.kt` has no error-state UI — a failed relationship fetch renders a blank screen.
- Camera-permission-denied screen is a single unstyled line with no recovery action.
- The Us tab and the gear-icon Settings screen still diverge in features (anniversary date and vibe controls only exist in one of the two).
- Splash screen still unbranded on API 26–30 (within the app's own `minSdk = 26`), with a related purple status-bar flash at cold start from leftover default template colors.
- Notification permission is requested immediately on first launch with no rationale screen.
- Install Referrer data collection isn't disclosed in the privacy policy.
- Invite-code/referral deep link is fully dead end-to-end — captured on install or via nav-graph deep link, but never read back anywhere.
- Privacy policy/terms still missing a data-retention-period statement and a minimum-age/children's-privacy clause.
- A dead, unused `EmotionalActionMenu` composable exists with zero `contentDescription` on any of its emoji buttons — not live, but a landmine if anyone wires it up later without noticing.

## Maintainability (LOW)
- `UpdateThemeAsync` fully implemented server-side, never wired to a controller endpoint.
- `RelationshipStatus.Pending` enum value declared, never used anywhere.
- Unbounded `limit` query param on the scrapbook endpoint.
- Compose BOM (`2023.10.01`) and Hilt (`2.50`) are roughly two years old against `compileSdk 36`.
- Leftover default Android Studio template files (`ic_launcher_background.xml`/`ic_launcher_foreground.xml` vector drawables) are unused dead resources — the real adaptive icon uses the actual PNG mipmaps instead.

---

Add new items here as they come up rather than letting them scatter across chat history — this is now the one place to check before starting a "what's left" pass.
