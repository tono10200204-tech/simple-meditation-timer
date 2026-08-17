// Kotlin support is built into AGP 9, so no org.jetbrains.kotlin.android here.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
