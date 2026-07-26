# Backend Deploy Process

Migrations are **not** applied automatically on startup (see the comment in `Program.cs`).
Auto-migrating on every boot is unsafe once you have more than one instance running: two
processes can race to apply the same migration concurrently, and a bad migration will
crash-loop every instance on every deploy with no lock or leader election protecting it.

## Required step on every deploy that includes a migration

Before the new app version starts serving traffic, run migrations as a separate,
one-shot step against the production database:

```bash
cd backend
dotnet tool restore   # first time only, installs dotnet-ef pinned in the project
dotnet ef database update --project Moment.Api.csproj
```

This needs the same `ConnectionStrings__DefaultConnection` the running app uses.

## Suggested CI/CD wiring

Whatever platform you deploy to (Railway/Render/etc.), add this as an explicit
pre-deploy or release-phase command, separate from the web process start command:

- Railway: use a "Release Command" / pre-deploy step if your plan supports it, or a
  one-off `railway run` invocation of the command above before promoting the new
  deployment.
- Render: use a "Pre-Deploy Command" in the service settings, set to the command above.
- Any other CI (GitHub Actions, etc.): add a job step that runs the migration command
  against the production connection string as part of the deploy job, before/independently
  of restarting the web service.

## Rolling back

If a migration needs to be rolled back, use:

```bash
dotnet ef database update <PreviousMigrationName> --project Moment.Api.csproj
```

Never delete a migration file that has already been applied to production - add a new
migration that reverses the change instead, so migration history stays consistent across
all environments.
