plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "app.tagwerk"
    compileSdk = 35

    // In GitHub Actions zählt jede Version hoch, damit Updates sich sauber installieren lassen.
    val buildNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

    defaultConfig {
        applicationId = "app.tagwerk"
        minSdk = 26
        targetSdk = 35
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"
    }

    // Fester Schlüssel für alle Builds: Updates passen so immer zur installierten App.
    // Für eine private App liegt er im Repository; siehe README.
    signingConfigs {
        create("tagwerk") {
            storeFile = rootProject.file("signing/tagwerk.jks")
            storePassword = "tagwerk"
            keyAlias = "tagwerk"
            keyPassword = "tagwerk"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("tagwerk")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("tagwerk")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    // echte org.json-Umsetzung für Unit-Tests (die aus android.jar ist nur ein Stub)
    testImplementation("org.json:json:20240303")
}
