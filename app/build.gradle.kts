import org.gradle.api.GradleException

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
}

fun parseAppVersion(versionName: String): Pair<Int, Int> {
    val match = Regex("""^(\d+)\.(\d+)$""").matchEntire(versionName)
        ?: throw GradleException("App version must match <major>.<minor>, for example 1.0.")
    val (majorValue, minorValue) = match.destructured
    val major = majorValue.toInt()
    val minor = minorValue.toInt()
    if (minor !in 0..9) {
        throw GradleException("Minor version must stay between 0 and 9 so versions roll like 1.0 -> 1.9 -> 2.0.")
    }
    return major to minor
}

fun stringPropertyOrEnv(propertyName: String, envName: String): String? {
    return providers.gradleProperty(propertyName).orNull
        ?: System.getenv(envName)?.takeIf { it.isNotBlank() }
}

val appVersionName = providers.gradleProperty("appVersionName").orElse("1.4").get()
val (majorVersion, minorVersion) = parseAppVersion(appVersionName)
val appVersionCode = majorVersion * 100 + minorVersion * 10
val releaseStoreFilePath = stringPropertyOrEnv("releaseStoreFile", "ANDROID_RELEASE_KEYSTORE_PATH")
val releaseStorePassword = stringPropertyOrEnv("releaseStorePassword", "ANDROID_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = stringPropertyOrEnv("releaseKeyAlias", "ANDROID_RELEASE_KEY_ALIAS")
val releaseKeyPassword = stringPropertyOrEnv("releaseKeyPassword", "ANDROID_RELEASE_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseStoreFilePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "com.yueliangmanle.danci"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.yueliangmanle.danci"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(requireNotNull(releaseStoreFilePath))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDir("src/main/jniLibs")
        }
        getByName("androidTest") {
            assets.srcDir("$projectDir/schemas")
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)

    kapt(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.json)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.room.testing)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}
