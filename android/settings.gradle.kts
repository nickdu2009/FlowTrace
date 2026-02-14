pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "flowtrace-android"

include(
  ":app",
  ":core:domain",
  ":core:application",
  ":core:infra:sunnynet",
  ":core:infra:storage",
)
