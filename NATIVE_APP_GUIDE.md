# JStudio Native App Guide: JT VOCAB QUIZ

This guide explains how to build the **JT VOCAB QUIZ** application as a standalone native Android app using **Kotlin** and **Jetpack Compose** directly on your phone using **JStudio**.

## Step 1: Set up JStudio
1. Download and install **JStudio** from the Play Store or official source.
2. Open JStudio and create a **New Project**.
3. Select **Kotlin/Compose** template (or a standard Kotlin project if you plan to add Compose manually).
4. **Project Settings**:
   - **Name**: JT VOCAB QUIZ
   - **Package Name**: `com.jtvocab.quiz`
   - **Minimum SDK**: 24

## Step 2: Configure build.gradle
In JStudio, open your `app/build.gradle` (or `build.gradle.kts`) and add the necessary Jetpack Compose dependencies:
```kotlin
dependencies {
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
}
```

## Step 3: Copy Source Files
JStudio allows you to create files and folders. Copy the code from the following files in this project into your JStudio project:

1. **`MainActivity.kt`**: Place in `java/com/jtvocab/quiz/`
2. **`VocabViewModel.kt`**: Place in `java/com/jtvocab/quiz/viewmodel/`
3. **`VocabRepository.kt`**: Place in `java/com/jtvocab/quiz/data/`
4. **`Models.kt`**: Place in `java/com/jtvocab/quiz/model/`

## Step 4: Run the App
- In JStudio, tap the **Play (Run)** icon.
- JStudio will compile the Kotlin code natively and generate an APK.
- Install and Open the app on your device.

---

## Native Source Code
The files provided in the `native-android/` folder of this project contain the production-ready code. You can copy-paste them directly into your Android Studio project.

### Key Logic Features
- **Local Persistence**: Uses SharedPreferences to save streaks, completed sets, and the "weak list" (words the user got wrong).
- **Jetpack Compose UI**: A modern, declarative UI with smooth transitions and Material 3 design.
- **Glassmorphism**: Replicates the "glass" look using high-contrast surfaces and subtle shadows.
