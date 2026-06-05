Moment — Product Requirements Document (PRD) v3
Document Status
Version: 3.0
Status: Draft Candidate for Gemini CLI Review
Owner: Product Team
Last Updated: 2026

1. Product Overview
   Product Name
   Moment
   Tagline
   Leave moments on someone's screen.
   Product Vision
   Moment allows trusted people to send meaningful wallpaper moments to each other.
   A sender chooses an image and a short note.
   The receiver's device automatically applies the wallpaper locally and records the event as a shared memory.
   Moment is not a wallpaper utility.
   Moment is an emotional presence product.

2. Core Product Principle
   Every feature must reinforce:
   "Someone thought about you."
   If a feature does not strengthen that feeling, it should not be included in MVP.

3. Problem Statement
   Modern communication is noisy.
   Messages get buried.
   Notifications are ignored.
   Moment creates a more meaningful communication channel by allowing trusted people to leave moments directly on each other's screens.

4. MVP Goal
   Validate whether users enjoy sharing emotional moments through wallpapers.
   The MVP succeeds if:
   • Users can connect successfully.
   • Users can send wallpaper moments.
   • Wallpapers apply reliably.
   • Users revisit their timeline.
   • Users continue sending moments after initial setup.

5. MVP Scope
   Included
   Authentication
   • Google Sign-In
   • Firebase Authentication
   • Backend-issued JWT
   User Profiles
   • Display Name
   • Username
   • Profile Picture
   • Optional Bio
   Connections
   • Invite Links
   • Connection Requests
   • Accept
   • Block
   • Revoke
   Wallpaper Moments
   • Single recipient
   • Image
   • Short note
   • Home screen target
   • Lock screen target
   • Both targets
   Timeline
   • Moment history
   • Status tracking
   • Thumbnail preview
   Notifications
   • Connection accepted
   • Moment received
   • Moment applied
   • Moment failed

6. Explicitly Excluded From MVP
   Do NOT implement:
   • Phone OTP authentication
   • Email search
   • Public user directory
   • Username search
   • Groups
   • Voice notes
   • AI-generated wallpapers
   • Couple streaks
   • Shared albums
   • Scheduled moments
   • Live wallpapers
   • Multiple recipients
   • Ask Every Time permissions
   These belong to future versions.

7. User Identity
   Authentication
   Google Sign-In only.
   Firebase Authentication is responsible for identity verification.
   Backend remains the source of truth for application users.

Profile Creation
Upon first login:
Automatically populate:
• Display Name
• Profile Picture
Allow user to edit:
• Display Name
• Profile Picture
• Username
• Bio

## First Time Onboarding Flow

Google Sign-In
→ Backend User Creation
→ Username Selection
→ Profile Review
→ Enter App

Requirements:

- Username is mandatory.
- Username must be unique.
- User cannot enter the application until a valid username is selected.
- Username availability must be validated by backend.

Username Rules
Format:
@username
Allowed characters:
• a-z
• 0-9
• underscore
Length:
• Minimum: 4
• Maximum: 20
Examples:
@pranay
@pranay_97
@momentlover
Usernames must be unique.

8. Discovery & Connections
   Discovery Method
   MVP uses Invite Links only.
   No public search functionality.
   No phone number lookup.
   No email lookup.

Invite Flow
User taps:
Invite Someone
App generates:
https://{APP_DOMAIN}/invite/{inviteCode}
Invite screen contains:
• Invite Link
• Copy Link button
• Share Invite button
Use Android native share sheet.
Supported share destinations include:
• WhatsApp
• Telegram
• Messages
• Gmail
• Any installed share target

Deep Link Behavior
If app installed:
Open app directly.
If app not installed:
Open Play Store.
After installation:
Continue invite flow automatically.

Connection Flow
Sender shares invite.
Receiver opens invite.
Receiver reviews profile.
Receiver taps Connect.
Connection request created.
Sender receives request.
Sender accepts.
Connection becomes active.

## Connection Consent

Before accepting a connection request, the receiver must see:

"By connecting with @username, you are allowing this user to send wallpaper moments to your device. You can revoke access at any time."

Actions:

- Accept
- Decline

Purpose:

- Transparency
- User trust
- Play Store compliance

9. Permissions
   MVP Permission Mode
   Only one mode:
   Always Allow
   Once granted:
   Trusted sender may send wallpaper moments without approval prompts.

Future Permission Mode
Not MVP:
Ask Every Time
Do not implement.

10. Wallpaper Moments
    Definition
    A Moment is:
    • Wallpaper image
    • Short note
    • Sender
    • Recipient
    • Timestamp

Moment Creation
Sender selects:
• Image
• Short note
• Wallpaper target
Targets:
• Home Screen
• Lock Screen
• Both
Then sends.

Note Limits
Maximum:
250 characters
Text only.
No markdown.
No links.

Image Preview & Cropping

Before sending:

Select Image
→ Preview
→ Crop
→ Select Target
→ Send

Supported Ratios:

- Original
- 16:9
- 18:9

Purpose:

- Better wallpaper quality
- Reduce unexpected cropping

11. Timeline
    Purpose
    Timeline is a memory layer.
    Timeline exists to preserve emotional moments after wallpapers change.

Timeline Item
Each entry displays:
• Thumbnail
• Sender
• Note
• Timestamp
• Delivery Status
Statuses:
• Delivered
• Applied
• Failed

Timeline Philosophy
Timeline should feel like:
A digital memory journal.
Not:
A technical activity log.

Additional Actions

- Report Moment
- Hide Moment
- Block User

MVP Report Flow

Report
→ Create Report Record
→ Notify Administrators

No moderation dashboard required for MVP.

Timeline Optimization

Backend generates:

- Original Image
- Thumbnail Image

Timeline loads thumbnails only.

Full image loads on demand.

## Wallpaper Recovery

Before applying a new wallpaper:

- Backup currently active wallpaper
- Store reference locally

Settings must include:

Restore Last Wallpaper Applied By Moment

Purpose:

- User trust
- Easy rollback
- Better onboarding conversion

## Wallpaper Change Notification

Whenever a wallpaper is successfully applied:

Notification:

✨ New Moment Applied

Sent by {DisplayName}

Tap to View

Purpose:

- Transparency
- User awareness
- Play Store compliance

12. Technical Stack
    Android
    • Kotlin
    • Jetpack Compose
    • MVVM
    • Clean Architecture
    • Hilt
    • Retrofit
    • Coroutines
    • StateFlow
    • Room
    • WorkManager
    • Coil
    • Firebase Auth
    • Firebase FCM
    • Firebase Crashlytics
    • Google Play Install Referrer API
    Backend
    • ASP.NET Core 8
    • Entity Framework Core
    • PostgreSQL
    • JWT Authentication
    Hosting
    • Railway
    Storage
    • Cloudflare R2

## Upload Strategy

Use Pre-Signed Upload URLs.

Flow:

Client
→ Request Upload URL
→ Upload Directly To R2
→ Notify Backend

Benefits:

- Lower backend load
- Lower Railway cost
- Better scalability

13. Architecture Principles
    Required:
    • MVVM
    • Repository Pattern
    • Use Cases
    • StateFlow
    • Clean Architecture
    Forbidden:
    • Business logic in Composables
    • GlobalScope
    • Direct Room access from UI
    • Direct API calls from UI

14. Android Reliability
    Wallpaper application must occur locally.

## Wallpaper Recovery Limitation

Android does not guarantee access to wallpapers that were not previously applied by the Moment application.

Moment must not attempt to read or restore arbitrary system wallpapers.

Supported:

- Restore Last Wallpaper Applied By Moment

Not Supported:

- Restore wallpaper that existed before Moment installation
- Restore wallpapers applied by other applications

  Backend must never attempt to directly modify device wallpaper.
  Receiver device responsibilities:
  • Receive FCM
  • Download image
  • Validate image
  • Apply wallpaper
  • Update status
  Use WorkManager for:
  • Downloads
  • Retries
  • Delivery confirmation

## FCM Recovery Strategy

FCM is best-effort.

Whenever app launches:

- Sync pending moments
- Sync delivery states

Endpoint:

GET /api/moments/pending

Purpose:

- Recover missed notifications
- Handle OEM battery restrictions
- Improve reliability

## Duplicate Protection

A Moment may arrive through:

- Firebase FCM
- Pending Moment Sync

Requirements:

- MomentId must be UNIQUE in local Room database.
- Duplicate processing must be ignored.
- Wallpaper must never be applied twice.

15. Non-Functional Requirements
    API Response Time:
    < 500 ms
    Wallpaper Delivery Success:
    90%
    Crash Free Sessions:
    99%
    Image Upload:
    < 5 seconds
    App Startup:
    < 2 seconds
    Timeline Load:
    < 2 seconds

16. Abuse Prevention
    Limits:
    • 20 moments/day
    • 50 invites/day
    • 5 moments/hour
    Required:
    • JWT validation
    • Permission validation
    • Rate limiting
    • Report Moment
    • Report User
    • Block User
    • Account Deletion
    • Revoke access

## Account & Data Deletion

Settings must provide:

- Delete Account
- Delete My Data

Delete process:

- Revoke permissions
- Remove connections
- Remove uploaded media
- Delete original images from Cloudflare R2
- Delete generated thumbnails from Cloudflare R2
- Soft delete user
- Queue background cleanup

Required for Google Play compliance.

# Play Store Compliance

Required:

- Report User
- Report Moment
- Block User
- Account Deletion
- Privacy Policy
- Terms of Service
- Wallpaper Change Notification

Moment must never change wallpapers silently without informing the receiver.

17. UI Direction
    The UI must feel:
    • Emotional
    • Cinematic
    • Premium
    • Warm
    • Intimate
    Avoid:
    • Corporate dashboards
    • Utility-app appearance
    • Meme aesthetics
    • Social-network clutter
    Visual inspiration:
    • Dark backgrounds
    • Warm coral gradients
    • Soft purple accents
    • Large typography
    • Rounded cards
    • Generous spacing
    Primary emotion:
    "Someone remembered me."

18. Future Roadmap
    v1.5
    • Username search
    • Ask Every Time permissions
    v2
    • Voice notes
    • Shared albums
    • Couple timelines
    • Scheduled moments
    Not Planned
    • Public social feed
    • Followers
    • Likes
    • Trending users

19. Gemini CLI Instructions
    Read this PRD completely before generating code.
    Do not invent features.
    Do not generate placeholder implementations.
    Generate:
    • Complete DTOs
    • Complete database migrations
    • Complete API contracts
    • Complete ViewModels
    • Complete repositories
    Prioritize:
    Reliability > Features
    Emotion > Complexity
    MVP Focus > Future Ideas

20. MVP Success Criteria
    The MVP is successful if:
    • A user can invite another user.
    • The invited user can connect successfully.
    • A moment can be sent.
    • Wallpaper applies successfully.
    • Timeline records the moment.
    • Users continue sending moments after connecting.
    The goal is validating emotional engagement.

# 21. Database Schema

## Users

| Field             | Type     |
| ----------------- | -------- |
| Id                | UUID     |
| FirebaseUid       | String   |
| Email             | String   |
| Username          | String   |
| DisplayName       | String   |
| ProfilePictureUrl | String   |
| Bio               | String   |
| TermsAcceptedAt   | DateTime |
| PrivacyAcceptedAt | DateTime |
| CreatedAt         | DateTime |
| UpdatedAt         | DateTime |

---

## Connections

| Field          | Type     |
| -------------- | -------- |
| Id             | UUID     |
| SenderUserId   | UUID     |
| ReceiverUserId | UUID     |
| Status         | Enum     |
| CreatedAt      | DateTime |
| UpdatedAt      | DateTime |

Connection Status:

```txt
PENDING
ACCEPTED
BLOCKED
REVOKED
```

---

## Devices

| Field      | Type     |
| ---------- | -------- |
| Id         | UUID     |
| UserId     | UUID     |
| FcmToken   | String   |
| Platform   | String   |
| DeviceName | String   |
| LastSeenAt | DateTime |
| CreatedAt  | DateTime |

Purpose:

Store active device registrations for push notifications.

---

## Invites

| Field        | Type     |
| ------------ | -------- |
| Id           | UUID     |
| InviteCode   | String   |
| SenderUserId | UUID     |
| CreatedAt    | DateTime |
| ExpiresAt    | DateTime |
| IsUsed       | Boolean  |

Requirements:

- InviteCode must be unique.
- Used invites cannot be reused.

---

## Moments

| Field           | Type     |
| --------------- | -------- |
| Id              | UUID     |
| SenderUserId    | UUID     |
| ReceiverUserId  | UUID     |
| ImageUrl        | String   |
| ThumbnailUrl    | String   |
| Note            | String   |
| WallpaperTarget | Enum     |
| Status          | Enum     |
| CreatedAt       | DateTime |
| DeliveredAt     | DateTime |
| AppliedAt       | DateTime |

WallpaperTarget:

```txt
HOME
LOCK
BOTH
```

MomentStatus:

```txt
PENDING
DELIVERED
APPLIED
FAILED
```

---

## Reports

| Field          | Type            |
| -------------- | --------------- |
| Id             | UUID            |
| ReporterUserId | UUID            |
| ReportedUserId | UUID (Nullable) |
| MomentId       | UUID (Nullable) |
| Reason         | String          |
| CreatedAt      | DateTime        |

Rules:

- Moment reports populate MomentId.
- User reports populate ReportedUserId.
- At least one of MomentId or ReportedUserId must be present.

---

## Notifications

| Field     | Type     |
| --------- | -------- |
| Id        | UUID     |
| UserId    | UUID     |
| Type      | String   |
| IsRead    | Boolean  |
| CreatedAt | DateTime |

---

# 22. API Versioning

All APIs must use:

```txt
/api/v1/
```

Examples:

```txt
/api/v1/auth
/api/v1/moments
/api/v1/connections
/api/v1/profile
```

## Device Registration API

POST /api/v1/profile/fcm-token

Request:

```json
{
  "fcmToken": "string"
}
```

Purpose:

Register device for push notifications.

---

# 23. Deep Link Specification

Invite links use:

```txt
https://{APP_DOMAIN}/invite/{inviteCode}
```

Example:

```txt
https://momentapp.in/invite/ABC123
```

Behavior:

If app installed:

- Open app directly
- Continue invite flow

If app not installed:

- Open Play Store
- Install app
- Continue invite flow automatically

## Deferred Deep Linking

## Deferred Deep Linking

Implementation:

- Android App Links
- Google Play Install Referrer API

Installed User Flow:

Invite Link
→ Open App
→ Continue Invite Flow

Not Installed User Flow:

https://{APP_DOMAIN}/invite/{inviteCode}

→ Backend/Web Layer receives inviteCode
→ Redirect to Play Store using Install Referrer

Example:

market://details?id={APPLICATION_ID}&referrer=inviteCode={inviteCode}

→ User installs app
→ App launches
→ Android retrieves Install Referrer
→ Extract inviteCode
→ Continue Invite Flow automatically

Requirements:

- Invite code must survive installation.
- Invite code must be recoverable after first launch.
- App must validate invite code with backend before proceeding.
- Invalid or expired invite codes must display an error state.

Purpose:

Prevent invite code loss during Play Store installation flow.

---

# 24. Supported Image Formats

Supported formats:

- JPEG
- PNG
- WEBP

Maximum upload size:

```txt
10 MB
```

Recommended upload size:

```txt
< 5 MB
```

Images must be compressed before upload.

Compression must occur on the Android client before upload to Cloudflare R2.

Requirements:

- Resize large images
- Compress images
- Optimize upload size

---

# 25. Terms & Privacy Acceptance

During first login:

User must accept:

- Terms of Service
- Privacy Policy

Store:

```txt
TermsAcceptedAt
PrivacyAcceptedAt
```

Users cannot continue without acceptance.

---

# 26. Backend Recommendations

Moment history will become the largest table.

Required indexes:

```txt
Moments(SenderUserId)
Moments(ReceiverUserId)
Moments(CreatedAt)
Moments(Status)

Connections(SenderUserId)
Connections(ReceiverUserId)
```

---

# 27. MVP Rule

If a feature does not directly improve:

1. Sending a Moment
2. Receiving a Moment
3. Remembering a Moment

It should not be included in MVP.

---

# 28. Final Founder Principle

Moment is not a wallpaper utility.

Moment is an emotional presence product.

Every screen should reinforce:

"Someone thought about you."

Emotion > Features

Reliability > Complexity

Shipping > Perfection
