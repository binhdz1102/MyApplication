plugins {
    id("launcher.android.library")
    id("launcher.android.hilt")
}

android {
    namespace = "com.example.myapplication.data.launcher"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":domain:launcher"))

    implementation(libs.coroutines.core)
}
