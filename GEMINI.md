# Moment - Project Instructions

## Overview
Moment is an emotional presence product allowing trusted people to share wallpaper moments (images + notes) directly to each other's device screens.

## Architecture
- **Monorepo**: Android and Backend live in the same repository.
- **Backend**: ASP.NET Core 8 Web API, Entity Framework Core, PostgreSQL, Firebase Admin SDK (Auth/FCM).
- **Android**: Kotlin, Jetpack Compose, MVVM + Clean Architecture, Hilt, Retrofit, Room, WorkManager (for wallpaper application).
- **Storage**: Cloudflare R2 for image hosting.

## Project Structure
- `/android`: Kotlin Android project (`com.moment.app`).
- `/backend`: ASP.NET Core API project (`Moment.Api`).
- `/backend/Models`: Database schemas (User, Connection, Invite, Device, WallpaperMoment, Report).
- `/backend/Services`: Core business logic (Auth, Connection, Moment, Timeline, Storage).

## Current State (June 2026)
- **MVP Implementation Complete**: All phases (0-4) from the implementation plan are finished.
- **Authentication**: Google Sign-In and mandatory username selection are implemented.
- **Connections**: Invite link system and connection management are functional.
- **Moments**: Core wallpaper sending and background application logic is implemented.
- **Timeline**: Paginated memory timeline and reporting tools are implemented.

## Development Workflow
- **Migrations**: Always use `dotnet ef migrations add <Name> --project Moment.Api.csproj` from the `backend/` directory.
- **Android DI**: Using Hilt. Ensure `@AndroidEntryPoint` and `@HiltAndroidApp` are used correctly.
- **Background Work**: The `WallpaperWorker` handles wallpaper updates. It requires `SET_WALLPAPER` and `INTERNET` permissions.

## GitHub Maintenance
- **CI/CD**: GitHub Actions are configured for path-filtered builds:
  - `android-ci.yml`: Triggers on `android/` changes.
  - `backend-ci.yml`: Triggers on `backend/` changes.
- **Automation**: Dependabot is enabled for weekly dependency updates.

## Key Files
- `prd.md`: Product Requirements Document.
- `SETUP.md`: Detailed setup and environment configuration guide.
- `plans/moment-mvp-implementation-plan.md`: The historical implementation roadmap and status.
