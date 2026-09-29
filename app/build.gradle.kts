import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.10"
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

fun sanitizeProperty(value: String?): String {
    return value?.trim()?.removeSurrounding("\"")?.removeSurrounding("'") ?: ""
}

val supabaseUrl = sanitizeProperty(localProperties.getProperty("SUPABASE_URL"))
val supabasePublishableKey = sanitizeProperty(localProperties.getProperty("SUPABASE_PUBLISHABLE_KEY"))
val supabaseSecretKey = sanitizeProperty(localProperties.getProperty("SUPABASE_SECRET_KEY"))
val supabaseJwksUrl = sanitizeProperty(localProperties.getProperty("SUPABASE_JWKS_URL"))
val geminiApiKey = sanitizeProperty(localProperties.getProperty("GEMINI_API_KEY"))

android {
    namespace = "com.MADproject.quoraforuniversities"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.MADproject.quoraforuniversities"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"$supabasePublishableKey\"")
        buildConfigField("String", "SUPABASE_SECRET_KEY", "\"$supabaseSecretKey\"")
        buildConfigField("String", "SUPABASE_JWKS_URL", "\"$supabaseJwksUrl\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.compose.material:material-icons-extended")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(
        platform("io.github.jan-tennert.supabase:bom:3.5.0")
    )
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.ktor:ktor-client-android:3.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
}