plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Version aus dem Git-Tag (Release-Workflow setzt VERSION_NAME): Datum des Releases Jahr.Monat.Tag, z. B. v26.11.27
val versionAusTag: String? = System.getenv("VERSION_NAME")?.removePrefix("v")?.takeIf { it.matches(Regex("\\d+\\.\\d+\\.\\d+(\\.\\d+)?")) }
val versionName0 = versionAusTag ?: "26.11.27"
val versionCode0 = versionName0.split(".").map { it.toInt() }.let { p -> p[0] * 1_000_000 + p[1] * 10_000 + p[2] * 100 + p.getOrElse(3) { 0 } }

android {
    namespace = "de.oejendorferdamm.dammkeyboard"
    compileSdk = 34

    defaultConfig {
        applicationId = "de.oejendorferdamm.dammkeyboard"
        minSdk = 26
        targetSdk = 34
        versionCode = versionCode0
        versionName = versionName0
        // nur Deutsch – spart die Übersetzungen aller Systembibliotheken
        resourceConfigurations += listOf("de")
    }

    // Wird nur in CI über Umgebungsvariablen gesetzt (siehe .github/workflows/release.yml).
    val releaseKeystorePfad = System.getenv("RELEASE_KEYSTORE_PATH")
    signingConfigs {
        if (releaseKeystorePfad != null) {
            create("release") {
                storeFile = file(releaseKeystorePfad)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
                storeType = "PKCS12"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releaseKeystorePfad != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    testOptions { unitTests.isReturnDefaultValues = true }
    packaging { resources.excludes += listOf("META-INF/*.version", "kotlin/**", "DebugProbesKt.bin") }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
