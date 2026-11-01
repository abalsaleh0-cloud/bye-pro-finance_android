plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }
  defaultConfig {
    applicationId = "com.aistudio.byeprofinance.kxmpzq"
    minSdk = 24
    targetSdk = 36
    versionCode = 10022
    versionName = "10.0.22"
    buildConfigField("String", "FINANCE_API_BASE_URL", "\"https://bye-pro-finance.vercel.app/\"")
    buildConfigField("String", "SUPABASE_URL", "\"https://ngfaukvfsesoslfiwaue.supabase.co\"")
    buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"sb_publishable_5zPaJKKmtGZtYUv1GGytjA_tsk9LaSR\"")
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }
  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath); storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = System.getenv("KEY_ALIAS") ?: "byeprofinance"; keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore"); storePassword = "android"; keyAlias = "androiddebugkey"; keyPassword = "android"
    }
  }
  buildTypes {
    release {
      isCrunchPngs = false; isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }
  buildFeatures { compose = true; buildConfig = true }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo { includeInApk = false; includeInBundle = true }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) } }
secrets { propertiesFileName = ".env"; defaultPropertiesFileName = ".env.example"; ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN") }
dependencies {
  implementation(platform(libs.androidx.compose.bom)); implementation(libs.androidx.activity.compose)
  implementation("com.google.ai.edge.litertlm:litertlm-android:0.18.0")
  implementation(libs.androidx.compose.material.icons.core); implementation(libs.androidx.compose.material.icons.extended)
  implementation("com.composables:icons-lucide-cmp:2.2.1"); implementation(libs.androidx.compose.material3); implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics); implementation(libs.androidx.compose.ui.tooling.preview); implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences); implementation(libs.androidx.lifecycle.runtime.compose); implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose); implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.work.runtime.ktx); implementation(libs.coil.compose); implementation(libs.converter.moshi)
  implementation(libs.kotlinx.coroutines.android); implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor); implementation(libs.moshi.kotlin); implementation(libs.okhttp); implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4); testImplementation(libs.androidx.core); testImplementation(libs.androidx.junit); testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test); testImplementation(libs.robolectric); androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4); androidTestImplementation(libs.androidx.espresso.core); androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner); debugImplementation(libs.androidx.compose.ui.test.manifest); debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.moshi.kotlin.codegen)
}