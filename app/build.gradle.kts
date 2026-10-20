import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Local, untracked configuration: sdk.dir, MEMOS_BASE_URL, MEMOS_TOKEN and the release keystore.
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun localOrEnv(key: String): String? =
    localProps.getProperty(key) ?: System.getenv(key)

fun quoted(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val memosBaseUrl = localOrEnv("MEMOS_BASE_URL").orEmpty()
val memosToken = localOrEnv("MEMOS_TOKEN").orEmpty()
val keystoreFile = localOrEnv("RELEASE_STORE_FILE").orEmpty()
val keystorePassword = localOrEnv("RELEASE_STORE_PASSWORD").orEmpty()
val keyAliasName = localOrEnv("RELEASE_KEY_ALIAS").orEmpty()
val keyPasswordValue = localOrEnv("RELEASE_KEY_PASSWORD").orEmpty()

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

android {
    namespace = "com.imaegoo.memoswatch"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.imaegoo.memoswatch"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        if (keystoreFile.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreFile)
                storePassword = keystorePassword
                keyAlias = keyAliasName
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (keystoreFile.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "MEMOS_BASE_URL", quoted(memosBaseUrl))
            buildConfigField("String", "MEMOS_TOKEN", quoted(memosToken))
        }
        debug {
            buildConfigField("String", "MEMOS_BASE_URL", quoted(memosBaseUrl))
            buildConfigField("String", "MEMOS_TOKEN", quoted(memosToken))
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = false
    }
}

dependencies {
}
