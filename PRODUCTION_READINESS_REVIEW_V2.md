# Moment — Production Readiness Review (Round 2)

**Date:** 2026-07-29
**Scope:** Full re-audit of `android/`, `backend/`, `website/public/` after all remediation from Round 1 (log.txt purge, rate limiting, session revoke, profile-picture validation, Terms/Privacy consent, widget/maintenance-screen fixes) plus fixes made live during this pass.
**Method:** 4 independent parallel audits (Android engineering, Backend .NET, Security/OWASP, UX+Play Store), each explicitly told what was already fixed and instructed to find anything new or still-broken, not to re-report closed items. Every CRITICAL and HIGH claim below was personally re-verified by direct file read against the actual current source before being written into this report — not passed through from the subagents on trust.

---

## Verdict

## 🔴 NOT READY FOR PRODUCTION — but the gap is now a short, concrete list, not a systemic problem

Round 1 found two CRITICAL issues (a real secret-leaking file in git history, hardcoded production credentials). Both are resolved. This round found three *new* CRITICAL engineering bugs — all three have already been fixed as part of writing this report, not just documented. What's left blocking a genuine "ship it" is one likely-broken compliance path that needs your confirmation, one unresolved architectural privacy tradeoff (public photo storage), and a punch list of real but non-blocking UX/accessibility/consistency issues.

---

## Scores (0–100)

| # | Category | Score | Direction vs. Round 1 |
|---|---|---|---|
| 1 | **Overall Production Readiness** | **71** | ▲ from 38 |
| 2 | UI | 74 | ▲ from 72 |
| 3 | UX | 66 | ▲ from 65 |
| 4 | Performance | 71 | ▲ from 60 |
| 5 | Security | 63 | ▲ from 25 |
| 6 | Architecture | 73 | ▲ from 70 |
| 7 | Scalability | 66 | ▲ from 55 |
| 8 | Maintainability | 66 | ▲ from 58 |
| 9 | Code Quality | 69 | ▲ from 60 |
| 10 | Play Store Readiness | 68 | ▲ from 55 |

Security moved the most because Round 1's two CRITICALs (leaked tokens in git history, hardcoded JWT/DB secrets) are both genuinely closed. It's not higher because a real HIGH item (public, unauthenticated photo storage) remains open, and this round found new MEDIUM items (unencrypted local DB, alpha-grade crypto dependency, FCM-token reassignment gap).

---

## Fixed during this pass (not just found — actually changed, verified balanced/consistent)

1. **`SendMomentViewModel.kt` — real OOM crash risk.** `inSampleSize` was computed to downscale large images before sending, but the computed value was never assigned back to `options.inSampleSize` before the actual decode call. Every Moment send fully decoded the original-resolution image into memory (a 12MP photo ≈ 48MB as ARGB_8888) before scaling it down afterward. On a large image or a lower-RAM device this throws `OutOfMemoryError`, which is a `java.lang.Error`, not an `Exception` — it was not caught by the surrounding `catch (e: Exception)` and would crash the app. One-line fix, now applied.
2. **`MomentDbContextModelSnapshot.cs` — the actual unfixed cause of last session's startup crash.** When the `RefreshToken`/`PreviousRefreshToken` indexes were added, the migration's own `Designer.cs` got them, `MomentDbContext.cs`'s Fluent API got them, but the shared snapshot file (what EF uses to detect "pending model changes" on every future startup) never did. That gap is the literal, specific thing that made `PendingModelChangesWarning` fire — it was papered over with `ConfigureWarnings(...Log(...))` rather than fixed. Now fixed at the source; the workaround should stay in place only as a safety net, not as the actual fix.
3. **`Program.cs` — rate limiter was running before authentication.** `app.UseRateLimiter()` was registered *before* `app.UseAuthentication()`. Every "per-user" rate limit policy (`JoinLimiter`, `EmotionalLimiter`, `BurstLimiter`) reads `context.User.FindFirst(ClaimTypes.NameIdentifier)` to partition by user — but at the point the rate limiter middleware ran, `HttpContext.User` was still the default *unauthenticated* principal, regardless of a valid JWT being present. Every one of those "per-user" limiters was silently always falling back to per-IP partitioning. On a shared/carrier-NAT IP (very common on mobile networks), this means one user's activity could exhaust the rate-limit bucket for everyone behind the same IP — reintroducing the exact class of problem the JoinLimiter-partitioning fix from Round 1 was meant to close. Reordered so authentication runs first.
4. **`RelationshipService.JoinRelationshipAsync` — pairing race condition could double-pair a user.** Invite redemption was a classic check-then-act with no transaction: read invite → check not-used/not-expired → do a bunch of other reads/checks → one `SaveChangesAsync()` at the very end. Two near-simultaneous redemptions of the same still-valid invite (a client retry after a dropped response, a double-tap, or two people opening a shared/leaked invite link close together) could both pass every check and each create a separate `Relationship` row — silently pairing the sender with two different partners at once, with no DB constraint to catch it. Now the invite is claimed via an atomic conditional `UPDATE ... WHERE IsUsed = false` (`ExecuteUpdateAsync`) that fails closed if another request already claimed it first. *Caveat: this closes the "same invite redeemed twice" case, the most likely real-world trigger. A narrower residual race — two different invites redeemed concurrently for the same pair of users — would need a serializable transaction with retry logic to fully close; not attempted here given the risk of making an untested, more invasive change to core pairing logic. Recommend real device/emulator testing of the pairing flow before treating this as bulletproof.*
5. **`AuthRepositoryImpl.updateProfile()` — same widget-staleness bug as the already-fixed `updateVibe()`.** Changing display name or profile picture updated the cached data the widget reads but never told the widget to repaint. Now calls `forceUpdate()`, same as every other mutation.
6. **`AuthRepositoryImpl.clearSession()` — incomplete logout cleanup + a revoke call that likely never completed.** Two issues, both fixed:
   - Logout only cleared session/refresh tokens, not the cached profile or cached relationship (same underlying prefs file). On a shared/family device, the next account logging in could briefly see the previous account's cached partner/relationship data if their own fetch failed offline. Now cleared, and the widget is repainted so a still-pinned widget doesn't keep showing the old partner.
   - The server-side session-revoke call (`api.logout()`, added in Round 1) was awaited inside a coroutine scoped to `AuthViewModel`'s `viewModelScope` — but the Logout button navigates away in the same click handler, immediately tearing down the nav-graph entry that owns that scope. In the common case this almost certainly cancelled the revoke call before it completed, meaning the Round 1 "add a logout endpoint" fix existed in code but often didn't actually run. Now fired in a detached coroutine (same pattern already used by the widget's `forceUpdate()`), so it completes regardless of navigation.
   - *Residual, intentionally not fixed:* this does not reset `RelationshipRepositoryImpl`'s in-memory state if the app process stays alive across a logout — that gets overwritten by the next login's own relationship fetch, so the exposure window is bounded to "between logout and the next login," not indefinite. A full fix would mean wiring a cross-repository "session cleared" signal, which felt like too large a change to make blind in this pass.

All five files were checked for brace/paren balance after editing; none of these changes have been compiled or run (no `dotnet`/Android build environment available in this sandbox) — please do a real build before relying on them as final.

---

## Open findings — not fixed, need a decision or more invasive work

### Needs your confirmation before I touch it
- **`website/public/delete-account.html` likely has a typo'd support email.** It reads `pranayburra66@gmail.com` — one character off from your known address (`pranayburra6@gmail.com`). This is the Play-Store-required fallback deletion path for users who've lost app access; if `pranayburra66@...` isn't a real, monitored inbox, that path silently goes nowhere. I did not change this myself since it's external contact information I can't verify — please confirm which address is correct.

### HIGH — open, real, not a quick one-liner
- **R2 photo/thumbnail storage is fully public and unauthenticated** (`StorageService.GetPublicUrl`, `MomentService.CreateMomentAsync`). Object keys are server-generated GUIDs — not guessable — but that's the *only* access control. Any URL that ever leaks (FCM payload contents sit in device logs / Room DB, a screenshot, a shared link) grants permanent, unrevocable viewing access to what is, for this specific product, the most sensitive content it handles — private couple photos. There's also no per-moment delete; only full account deletion cleans up R2 objects. This was flagged in Round 1 (as H-7) and remains open. Recommend short-lived signed URLs for full-resolution originals, or an explicit, documented product decision to accept this tradeoff for v1.

### MEDIUM — real, worth a fast follow-up round, not launch-blocking on their own
- Certificate pinning is **not actually implemented**, despite being marked done in the Round 1 task list — the code itself is honest about this (`network_security_config.xml` has a clear, well-reasoned comment explaining why it was deliberately deferred, given Railway-managed cert rotation risk). Correcting the record here, not reporting a new bug: this was a considered decision, not an oversight, but "done" in the tracker overclaimed it.
- Moment content (photos/notes) stored unencrypted in the local Room database. Session tokens are correctly protected via `EncryptedSharedPreferences`; the app's actual core sensitive content is not similarly protected. Requires root/physical device access to exploit — not remote.
- `androidx.security:security-crypto:1.1.0-alpha06` — the library backing your one properly-encrypted store is a long-lived pre-1.0 alpha. Works today; no stability guarantee.
- Device/FCM token reassignment (`DeviceService.RegisterDeviceAsync`) has no ownership check — a token already tied to one user gets silently reassigned to whoever registers it next. Bounded by how hard it'd be for an attacker to actually obtain someone else's live FCM token.
- Single refresh-token-per-user architecture: logging into the same account on a second device silently invalidates the first device's session (no per-device token storage, despite a `Device` table already existing), and two truly concurrent refresh calls can race with no conflict detection. May be an intentional single-session design — worth confirming it's a product decision, not an oversight.
- Widget bypasses the app's own shared Coil image cache (`RelationshipWidget.getBitmap` constructs a fresh `ImageLoader` every repaint instead of using the singleton `MomentApplication` already provides) — redownloads both profile photos from scratch on every widget update.
- Widget's "sending..." action-confirmation state is reverted via a `GlobalScope` coroutine with a 1200ms delay — if the process dies in that window, the widget can get stuck showing "😘 Sending a kiss" indefinitely until the next button tap.
- In-app "send a reaction" (Moments screen) has no retry/offline handling and silently swallows non-429 failures — the identical action from the widget goes through WorkManager with real retry. Two implementations of the same feature, one reliable, one not.
- `SpaceSettingsScreen.kt` has no error-state UI at all — a failed relationship fetch renders a blank screen.
- Google Sign-In cancellation/failure on the login screen produces zero user feedback — button just resets with no explanation.
- Camera-permission-denied screen is a single unstyled line of text with no recovery action, in an app that otherwise has real, designed empty states elsewhere.
- Two settings surfaces (Us tab vs. gear-icon `SpaceSettingsScreen`) still diverge in features — anniversary date and vibe controls only exist in one of the two paths.
- Splash screen still unbranded on API 26–30 (within the app's own `minSdk = 26`) — confirmed unchanged from Round 1.
- Text contrast: `TextMuted` (~3.8:1) and several alpha-reduced labels fall under WCAG AA's 4.5:1 threshold, used broadly including the very first screen (Login).
- No `contentDescription` on the image-editor color swatches or the five quick-reaction emoji buttons — real TalkBack gaps.
- Invite-code/referral deep-link flow is confirmed still fully dead end-to-end (captured on install, saved to disk, never read back by anything) — unchanged from Round 1.
- Privacy policy and Terms are missing a data-retention-period statement and a children's-privacy/minimum-age clause — standard disclosures, currently absent from both documents.
- Compose BOM (`2023.10.01`) and Hilt (`2.50`) are roughly two years old against `compileSdk 36` — not confirmed broken (no build environment available here to compile and check), but a real risk factor for the first real build attempt.

### LOW — worth a mention, not worth blocking on
`UpdateThemeAsync` fully implemented server-side with no controller endpoint ever wired to it (dead code, or a missing feature — worth a decision either way); `RelationshipStatus.Pending` enum value never used anywhere; unbounded `limit` query param on the scrapbook endpoint with no rate limit; `IsUsernameAvailable` endpoint has neither `[Authorize]` nor rate limiting (unlimited anonymous username enumeration); missing max-length validation on a couple of DTOs; no `UseHsts()` in the pipeline (low-impact given the client hardcodes HTTPS); Google Sign-In nonce generated client-side but never validated server-side (defense-in-depth gap only); redundant double `forceUpdate()` call in the FCM vibe handler; `joinRelationship()` doesn't call `forceUpdate()` directly (currently masked by an immediate follow-up refresh call that does); no `NetworkType.CONNECTED` constraint on the widget's presence-send work request; missing privacy-policy sections for user-rights contact, international data transfer, and governing law; feature graphic/screenshots for the Play listing don't exist yet anywhere (expected — a Play Console task, not a code gap).

---

## What's genuinely solid (checked, not just assumed)

- **Authorization**: every resource-mutating endpoint reviewed across Moment, Relationship, Report, Presence, and Auth controllers derives the acting user strictly from the JWT and re-verifies ownership before mutating anything — no IDOR found anywhere in this pass.
- **JWT/token design**: HS256 only (no algorithm-confusion surface), 15-minute access tokens, refresh tokens are 64 random bytes hashed at rest, issuer/audience/lifetime all validated.
- **Android token storage**: session/refresh tokens are in real `EncryptedSharedPreferences` (AES-256-GCM/SIV) with a hard failure (no plaintext fallback) if the Keystore ever fails.
- **Communication security**: cleartext traffic disabled app-wide, no custom TrustManager/HostnameVerifier bypass anywhere, HTTPS hardcoded.
- **Business-logic guards**: moment creation and presence signals both re-check relationship status/pause flags server-side at write time — a stale or blocked relationship can't be used to send content.
- **`DeleteAccountAsync`**: correctly transactional, correctly nulls the FK before deleting moments, treats storage cleanup as best-effort-after-commit rather than risking a half-deleted account.
- **Global exception handling**: positioned to wrap the whole pipeline, returns a fixed generic message — no stack-trace leakage found anywhere.
- **AndroidManifest permissions**: lean, every permission justified and evidenced in code, nothing unexplained.
- **Backend dependency versions**: all current .NET 10 GA-era packages, nothing identifiably vulnerable.

---

## Bottom line

Round 1 closed the two things that made this a hard no (secrets in git history, hardcoded production credentials). This round found and fixed three genuinely new CRITICAL bugs before they could ship (an OOM crash on every large photo send, rate limiting that was silently non-functional for its stated purpose, and a real pairing race condition) — all three were live in the codebase, not theoretical. What's left is one email address that needs your eyes, one unresolved storage-privacy tradeoff worth a real product decision, and a substantial but non-blocking punch list of UX/accessibility/consistency work. This is a materially different, much shorter conversation than Round 1's.
