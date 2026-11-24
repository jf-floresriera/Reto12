// Build file a nivel de proyecto. No declara dependencias propias, solo los
// plugins que los módulos (app/) pueden aplicar.
plugins {
    id("com.android.application") version "8.6.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    // Desde Kotlin 2.0, el compilador de Compose es un plugin de Gradle aparte
    // (ya no alcanza con composeOptions { kotlinCompilerExtensionVersion }).
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
