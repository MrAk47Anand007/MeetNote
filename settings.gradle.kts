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

rootProject.name = "MeetNote"

include(":androidApp")
include(":android-core")
include(":android-capture")
include(":android-background")
include(":android-security")
include(":android-ai-local")
include(":android-asr")
include(":shared:core")
include(":shared:domain")
include(":shared:storage")
include(":shared:providers")
include(":shared:ai-contracts")
include(":shared:export")
