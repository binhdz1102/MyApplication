plugins {
    id("launcher.android.library")
}

android {
    namespace = "com.example.myapplication.domain.launcher"
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.coroutines.core)
    implementation("javax.inject:javax.inject:1")
}
