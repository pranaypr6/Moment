# Moment — Production Readiness Review (Round 3)

**Date:** 2026-07-30
**Scope:** Full re-audit of `android/`, `backend/`, `website/public/` after all Round 1 + Round 2 fixes (log.txt purge from `main`, rate limiting, session revoke, R2 presigned URLs, pairing-race fix, widget staleness fixes, device push-token registration fix).
**Method:** 4 independent parallel audits (Android engineering, Backend .NET, Security/OWASP, UX+Play Store), each briefed on everything already fixed and instructed not to re-report closed items. Every CRITICAL and HIGH claim below was personally re-verified against the actual current source/repo — not passed through from the subagents on trust. The one CRITICAL finding below was independently confirmed by directly inspecting the git object it refers to.

---

## Verdict

## 🔴 NOT READY — one live, active CRITICAL exposure, but it's a fast fix, not a code problem

Every CRITICAL from Round 2 (OOM crash, broken rate limiting, pairing race) and the HIGH from Round 2 (public R2 bucket) are confirmed fixed and holding up under fresh, skeptical re-review. This round surfaced one new, genuinely urgent problem: **the earlier `log.txt` purge only rewrote your local `main` branch — the original, unmodified commit is still live on GitHub via 6 other branches**, and it contains a real backend session token that is still inside its validity window as of today. Everything else found this round is real but non-blocking.

---

## Scores (0–100)

| # | Category | Score | Direction vs. Round 2 |
|---|---|---|---|
| 1 | **Overall Production Readiness** | **67** | ▼ from 71 (new active CRITICAL) |
| 2 | UI | 73 | ▼ from 74 |
| 3 | UX | 65 | ▼ from 66 |
| 4 | Performance | 71 | — |
| 5 | Security | 55 | ▼ from 63 (new CRITICAL outweighs closed R2 HIGH) |
| 6 | Architecture | 74 | ▲ from 73 |
| 7 | Scalability | 66 | — |
| 8 | Maintainability | 65 | ▼ from 66 (more dead code found) |
| 9 | Code Quality | 69 | — |
| 10 | Play Store Readiness | 66 | ▼ from 68 |

Security dropped despite real progress because a live, currently-exploitable exposure outweighs everything that got fixed underneath it. Once the branch cleanup below is done, this swings back up sharply — the underlying app security posture (auth, IDOR, token storage, TLS) is unchanged and was re-verified clean.

---

## 🔴 CRITICAL — do this first

**The `log.txt` purge from Round 1 only touched `main`. The original commit with real leaked credentials is still publicly reachable on GitHub right now**, via 6 branches that were never rewritten:

```
temp-audit-fixes
feature/revenuecat-integration
feature/rename-package-com-pranayburra-moment
dependabot/github_actions/actions/setup-dotnet-6
dependabot/gradle/android/android-deps-fd2239ed74
dependabot/nuget/backend/backend-deps-a4370e63d5
```

I independently pulled the actual blob these branches point to (not just trusting the audit's claim) and confirmed it contains:
- A real Google OAuth id_token and a real backend-issued session token (JWT), both with full email/username/user-ID attached.
- The backend token's expiry decodes to **2026-08-08** — meaning as of today (2026-07-30) **it is still valid and usable against your live API**. This predates the fix that shortened access tokens to 15 minutes, so it's an old-format 30-day token that's simply still ticking.

**What you need to do (I can't push, so this has to happen from your machine):**
1. Delete those 6 branches from GitHub. All of them are stale (old feature work, temp/dependabot branches) — you already told me earlier this session you don't need branches like these, so this should be a clean, no-loss deletion: `git push origin --delete <branch-name>` for each, or delete them from the GitHub UI directly.
2. Consider rotating your `Jwt:Key` (the signing secret) on the backend. It's the only way to invalidate that specific already-issued token before its natural Aug 8 expiry — access tokens are stateless and aren't checked against a revocation list, so nothing else short of the key changing will kill it early. The tradeoff: rotating it logs out every currently-signed-in session (everyone has to sign in again), which is cheap to accept right now given the app isn't live yet.
3. Once the branches are gone, it's worth deleting the corresponding local remote-tracking refs too, but that's just cleanup — the GitHub-side deletion is what actually matters.

I did not delete these branches myself since it's a GitHub-side, credential-requiring push action — this needs to come from you.

---

## Fixed and re-verified clean this round (not new work — confirming Round 2 fixes held up)

- `SendMomentViewModel.kt` OOM risk, `MomentDbContextModelSnapshot.cs` missing indexes, `Program.cs` rate-limiter-before-auth ordering, `RelationshipService` pairing race, widget staleness (vibe/profile/anniversary), `clearSession()` cleanup — all re-read against current source, all correct, no regressions.
- R2 public bucket exposure — closed. All photo/thumbnail/profile-picture URLs now route through 24h presigned GET URLs. (You still need to flip "Public Development URL" to disabled in the Cloudflare dashboard yourself — not verifiable from code.)
- Device push-token registration fixed this session (detached scope so nav-away doesn't cancel the FCM registration call) — re-verified correct.

---

## New findings this round

### HIGH
- **Image-editor color swatches (`ImageEditorScreen.kt`, `EditorBottomBar`) are both unlabeled *and* too small for accessibility** — 32dp touch targets, below Android's 48dp minimum, with zero `contentDescription`. A real TalkBack + motor-accessibility double failure on a core editing screen.
- **Login screen text contrast is worse than previously measured** — `"Secure sign-in with Google"` (`LoginScreen.kt:141-143`) renders at roughly **2.3:1** contrast, and `"Privacy first. Always."` (line 156-159) at roughly **1.8:1** — both fail even WCAG AA's relaxed large-text floor (3:1), on the very first screen every user sees.

### MEDIUM
- **A successful in-app reaction send shows zero confirmation, even though the code has a success-toast path.** `MomentsScreen.kt:340` gates the toast on `actionSuccessState?.contains("too many") == true` — that's only ever true for the rate-limit-error case, so a genuine successful send never shows anything. Compounds the already-known "no retry on failure" gap into "no feedback either way."
- **`DeviceController.RegisterDevice` has no rate limiting**, unlike every other mutating endpoint in the app. Combined with the already-known token-reassignment gap (no ownership check when an FCM token gets re-claimed), this removes the one thing that would've throttled abuse of that gap.
- **Missing max-length validation on `RegisterDeviceRequest.FcmToken` and `JoinRelationshipRequest.PairingKey`** can trigger an uncaught Postgres index-size error (surfaces as a raw 500 instead of a clean 400) if a client sends an oversized value.
- **Small check-then-act race in moment hourly/daily rate limiting** (`MomentService.cs`) — two concurrent sends can both pass the count check before either commits. Low real-world impact since `BurstLimiter` already caps the blast radius.
- **No burst-rate-limit on pairing-key creation** — every other sensitive action has both a per-minute limiter and a business cap; this one only has the daily cap, so it can be exhausted in seconds.
- **Install Referrer data collection isn't disclosed in the privacy policy** — the app does read Play's Install Referrer API for invite-code attribution; the privacy policy doesn't mention it. Matters for Play Console's Data Safety form accuracy.
- **Notification permission is requested immediately on first launch with zero explanation** — no rationale screen, just a cold system dialog on a fresh install. Not a rejection risk, just measurably lowers opt-in rates.
- Small, low-severity items also found: a leftover purple status-bar flash at cold start (same root cause as the unbranded splash screen), `RelationshipRepositoryImpl`'s in-memory relationship state isn't reset on logout if the app process stays alive (already partially mitigated, narrow window), `InstallReferrerClient`'s connection is never closed on non-OK response codes (one-shot leak at cold start), and a dead, unused `EmotionalActionMenu` composable with no `contentDescription` at all sitting unreferenced in the codebase (landmine if anyone wires it up later without noticing).

---

## Everything from Round 2's open list — re-verified, still true, still open (nothing here is new)

Moment photos/notes unencrypted in local Room DB · `security-crypto:1.1.0-alpha06` alpha dependency · device/FCM token reassignment has no ownership check · single refresh-token-per-user architecture (no per-device sessions despite a `Device` table existing) · `UpdateThemeAsync` fully implemented server-side, never wired to a controller · `RelationshipStatus.Pending` never used · unbounded `limit` on the scrapbook endpoint · `IsUsernameAvailable` has no auth or rate limit (anonymous username enumeration) · Google Sign-In nonce generated client-side, never validated server-side (and structurally can't be — the DTO never transmits it) · widget rebuilds a fresh Coil `ImageLoader` every repaint instead of reusing the app's shared one · widget's "sending..." state reverts via `GlobalScope` with no crash-safety · in-app reaction send has no retry/offline handling · `SpaceSettingsScreen.kt` has no error-state UI · Google Sign-In cancellation gives zero user feedback · camera-permission-denied screen is a single unstyled line with no recovery action · Us tab and gear-icon Settings still diverge in features (anniversary/vibe only in one) · splash screen still unbranded on API 26–30 · invite-code/referral deep link fully dead end-to-end (confirmed on both entry paths this round: neither the install-referrer-saved code nor the nav-graph deep-link argument is ever read back anywhere) · privacy policy/terms missing retention period, minimum-age/children's clause, user-rights contact, international transfer, and governing-law sections · Compose BOM (2023.10.01) / Hilt (2.50) ~2 years old against `compileSdk 36` · no `UseHsts()` in the backend pipeline.

None of these are launch-blocking on their own; they're the same punch list as before, just reconfirmed rather than newly discovered.

---

## Bottom line

The engineering foundation is holding up: every CRITICAL bug from the last two rounds is fixed and re-verified, not just claimed. The one thing standing between this app and "ready" is a fast, mechanical cleanup — deleting 6 stale branches from GitHub and deciding whether to rotate the JWT signing key — not a code fix. Do that, confirm the Cloudflare "Public Development URL" toggle is off, and this becomes a genuinely short punch list of accessibility/polish/consistency items rather than a blocker list.
