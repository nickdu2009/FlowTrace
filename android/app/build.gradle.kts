plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.hilt)
  alias(libs.plugins.ksp)
}

android {
  namespace = "com.flowtrace"
  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig {
    applicationId = "com.flowtrace"
    minSdk = libs.versions.minSdk.get().toInt()
    targetSdk = libs.versions.targetSdk.get().toInt()
    versionCode = 1
    versionName = "0.1.0-alpha"

    vectorDrawables {
      useSupportLibrary = true
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlin {
    compilerOptions {
      jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

dependencies {
  // Internal modules
  implementation(project(":core:domain"))
  implementation(project(":core:application"))
  implementation(project(":core:infra:sunnynet"))
  implementation(project(":core:infra:storage"))

  // Kotlin
  implementation(libs.kotlin.stdlib)
  implementation(libs.kotlinx.coroutines.android)

  // AndroidX Core
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.ktx)
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

  // Compose
  val composeBom = platform(libs.compose.bom)
  implementation(composeBom)
  implementation(libs.compose.ui)
  implementation(libs.compose.ui.graphics)
  implementation(libs.compose.ui.tooling.preview)
  implementation(libs.compose.material3)
  implementation(libs.androidx.activity.compose)
  debugImplementation(libs.compose.ui.tooling)

  // Hilt
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)
  // Fix: Hilt/Dagger metadata parsing for Kotlin 2.3.x
  // See: "Provided Metadata instance has version 2.3.0, while maximum supported version is 2.2.0"
  ksp(libs.kotlin.metadata.jvm)
  annotationProcessor(libs.kotlin.metadata.jvm)
  implementation(libs.hilt.navigation.compose)

  // Logging
  implementation(libs.timber)
}
