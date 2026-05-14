# Build Guide for JT VOCAB QUIZ (Mobile)

To fix the GitHub errors and build your app natively, follow these steps:

## 1. Export these files to GitHub
The "No file matches" error on GitHub means you haven't pushed the Android source code yet.
1. Tap the **Sync/GitHub** menu in AI Studio.
2. Select **Export to GitHub** or **Push**. This will upload the `app`, `build.gradle`, and `.github` folders I just created.

## 2. GitHub Actions (The Build)
I have created a new file at `.github/workflows/android.yml`. This workflow:
- Automatically installs Gradle (fixes the "gradlew" error).
- Uses **JDK 17** (required for modern Android apps).
- Generates an APK for you in the cloud.

**How to find your APK:**
1. Open your GitHub repository in your browser.
2. Tap the **Actions** tab.
3. Tap on the latest **"Android CI"** run.
4. Once finished, scroll down to **"Artifacts"** and tap the linked zip file to download your app.

## 3. Building in JStudio (Manual)
If you prefer building inside the JStudio app:
1. **Clone** your GitHub repository inside JStudio.
2. JStudio will find the `build.gradle` file in the root.
3. **Important:** Go to JStudio **Settings** and ensure the **JDK Version** is set to **17**.
4. Tap the **Run** icon.

### Technical Note
I have updated your source code to be compatible with standard Android directory structures.
- **Root**: `.github/`, `app/`, `build.gradle`, `settings.gradle`
- **Main App**: `app/src/main/java/com/jtvocab/quiz/`
- **Resources**: `app/src/main/AndroidManifest.xml`
