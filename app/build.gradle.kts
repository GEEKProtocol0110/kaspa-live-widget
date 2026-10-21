plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val playDistribution = providers.gradleProperty("playDistribution").orNull == "true"

android {
    namespace = "com.kaspa.livewidget"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kaspa.livewidget"
        minSdk = 26
        targetSdk = 36
        versionCode = 12
        versionName = "1.6.0"
        
        setProperty("archivesBaseName", "kaspa-widget-v$versionName")
    }

    buildFeatures { buildConfig = true }
    defaultConfig { buildConfigField("boolean", "DIRECT_UPDATES", (!playDistribution).toString()) }
    sourceSets.getByName("release").manifest.srcFile(if (playDistribution) "src/play/AndroidManifest.xml" else "src/direct/AndroidManifest.xml")
    sourceSets.getByName("debug").manifest.srcFile("src/direct/AndroidManifest.xml")

    val signingEnvironment = listOf("WIDGET_KEYSTORE_PATH", "WIDGET_STORE_PASSWORD", "WIDGET_KEY_ALIAS", "WIDGET_KEY_PASSWORD")
        .associateWith { System.getenv(it)?.takeIf(String::isNotBlank) }
    require(signingEnvironment.values.all { it == null } || signingEnvironment.values.all { it != null }) {
        "Release signing requires all four WIDGET signing environment variables"
    }
    if (signingEnvironment.values.all { it != null }) {
        signingConfigs.create("persistentRelease") {
            storeFile = file(signingEnvironment.getValue("WIDGET_KEYSTORE_PATH")!!)
            storePassword = signingEnvironment.getValue("WIDGET_STORE_PASSWORD")
            keyAlias = signingEnvironment.getValue("WIDGET_KEY_ALIAS")
            keyPassword = signingEnvironment.getValue("WIDGET_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            if (signingEnvironment.values.all { it != null }) signingConfig = signingConfigs.getByName("persistentRelease")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.testLogging.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    
    // WorkManager for scheduled updates
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Networking for API calls
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    testImplementation("com.squareup.okhttp3:mockwebserver:4.11.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation("androidx.work:work-testing:2.9.0")
}
