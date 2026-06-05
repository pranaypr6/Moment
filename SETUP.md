# Moment - Setup & Installation Guide

This guide will help you get the **Moment** MVP (Backend & Android) up and running on your local machine and development environment.

## 1. Prerequisites
Ensure you have the following installed:
- **.NET 10 SDK** (for the Backend)
- **PostgreSQL** (running locally or accessible via network)
- **Android Studio** (Hedgehog or newer)
- **Firebase Account** (for Authentication and Cloud Messaging)
- **Cloudflare R2** (or any S3-compatible storage)

---

## 2. Backend Setup (`/backend`)

### A. Database & Secret Management
We use `appsettings.Development.json` for local configuration. This file is **ignored by Git** to ensure your keys stay on your machine.

1.  Open `backend/appsettings.Development.json`.
2.  Update the `ConnectionStrings:DefaultConnection` with your local PostgreSQL credentials.
3.  Set your local `Jwt:Key` and `Cloudflare` R2 details.

### B. Firebase Admin SDK
1.  Go to the [Firebase Console](https://console.firebase.google.com/).
2.  Project Settings > Service Accounts.
3.  Click **Generate New Private Key**.
4.  Save the `.json` file inside the `backend/` directory (e.g., `firebase-adminsdk.json`).
5.  In `appsettings.Development.json`, set the `Firebase:CredentialsPath` to your filename:
    ```json
    "Firebase": {
      "CredentialsPath": "firebase-adminsdk.json"
    }
    ```
    *Note: `*.json` files in the root are also ignored by git to keep this key safe.*

### C. Deployed Environments (Production)
In production, use **Environment Variables**. .NET automatically maps variables with `__` (double underscore) to config sections:
- `ConnectionStrings__DefaultConnection`
- `Firebase__CredentialsPath`
- `Jwt__Key`

Refer to `backend/.env.template` for the full list of required variables.

### E. Run Migrations & Start
1.  Open a terminal in the `backend/` folder.
2.  Install the EF Core tool: `dotnet tool install --global dotnet-ef`
3.  Apply migrations: `dotnet ef database update`
4.  Run the API: `dotnet run`
    The API will be available at `http://localhost:5200`.

---

## 3. Android Setup (`/android`)

### A. Firebase Config
1. In the [Firebase Console](https://console.firebase.google.com/), add an Android App.
2. Use the package name: `com.moment.app`.
3. Download the `google-services.json` file.
4. Place it in the `android/app/` directory.

### B. API Base URL
1. Open `android/app/src/main/java/com/moment/app/di/NetworkModule.kt`.
2. Update the `baseUrl` in the `provideRetrofit` function to point to your backend (use `10.0.2.2` if running on an Android Emulator):
   ```kotlin
   .baseUrl("http://10.0.2.2:5200/") // Local emulator address
   ```

### C. Google Sign-In (Credential Manager)
1. Go to the [Google Cloud Console](https://console.cloud.google.com/) -> APIs & Services -> Credentials.
2. Find the "Web application" OAuth 2.0 Client ID (usually auto-created by Firebase).
3. Copy that **Client ID**.
4. Open `android/app/src/main/res/values/strings.xml` and replace the placeholder with your actual Web Client ID:
   ```xml
   <string name="default_web_client_id">YOUR_WEB_CLIENT_ID.apps.googleusercontent.com</string>
   ```
5. Ensure you have added your **SHA-1 fingerprint** to the Firebase Project settings.

---

## 4. Cloudflare R2 (Storage)

1. Create a Bucket in Cloudflare R2.
2. Generate an **Access Key ID** and **Secret Access Key**.
3. Open `backend/appsettings.Development.json` and fill in the `Cloudflare` section:
   ```json
  "Cloudflare": {
    "AccountId": "your-id",
    "AccessKeyId": "your-key",
    "SecretAccessKey": "your-secret",
    "BucketName": "moment-assets",
    "PublicUrl": "https://pub-your-id.r2.dev"
  }
   ```

---

## 5. Running the Application

1. **Start the Backend**: `dotnet run` inside the `backend` folder.
2. **Build Android**: Open the `android` folder in Android Studio.
3. **Run Android**: Sync Gradle and run the `app` module on an emulator or physical device.

### Verification Checklist
- [ ] Backend starts without errors and Swagger is accessible at `/swagger`.
- [ ] Database tables are created.
- [ ] Android app opens to the Login Screen.
- [ ] Google Sign-In successfully redirects and creates a user in the DB.
- [ ] Username onboarding flow appears after the first login.
