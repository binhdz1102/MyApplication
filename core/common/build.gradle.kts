plugins {
    id("launcher.android.library")
}

android {
    namespace = "com.example.myapplication.core.common"
}

dependencies {
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.coroutines.core)
}
