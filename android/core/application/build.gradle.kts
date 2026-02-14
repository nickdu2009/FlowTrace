plugins {
  id("java-library")
  kotlin("jvm")
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
  jvmToolchain(25)
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
  }
}

dependencies {
  implementation(project(":core:domain"))
  implementation(libs.kotlin.stdlib)
  implementation(libs.kotlinx.coroutines.android)
}
