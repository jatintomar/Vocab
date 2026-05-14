# Build Guide for JT VOCAB QUIZ (Mobile)

I have fixed the "Cannot mutate dependencies" and "processDebugResources" errors. To build your APK now:

## 1. Export fixes to GitHub
The most important step is to push these new files:
1. Tap the **Sync/GitHub** menu in AI Studio.
2. Select **Export to GitHub** or **Push**.
   - This sends the new `res` folder (required for build) and the fixed `.github/workflows/android.yml`.

## 2. GitHub Actions (Automatic APK)
Once you push, the "Android CI" will run automatically.
- I fixed the command to use `./gradlew assembleDebug`.
- I added a step to **Upload APK**. Once the build finishes, you will see a "Artifacts" section in the GitHub Actions run where you can download the `.apk`.

## 3. If it still fails
The error was likely due to a version mismatch between Kotlin (`1.9.0`) and the Compose compiler (`1.5.1`). I have now aligned these.
If you see a "gradlew not found" error, it means you didn't push the `gradlew` script and `gradle/` folder. Ensure all files are exported.

### Technical Fix Summary
- **Resource Processing**: Created `app/src/main/res/values/` with string/color/theme definitions.
- **Workflow**: Updated to use the Gradle wrapper script with `chmod +x` permissions.
- **Versions**: Aligned Kotlin `1.9.0` with Compose Compiler `1.5.1`.
