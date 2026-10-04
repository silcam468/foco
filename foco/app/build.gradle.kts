plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.camila.focoapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.camila.focoapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"
    }

    // Clave fija para que cada compilación nueva se pueda instalar encima de la anterior
    // sin perder tus ajustes.
    signingConfigs {
        create("foco") {
            storeFile = file("foco.jks")
            storePassword = "foco1234"
            keyAlias = "foco"
            keyPassword = "foco1234"
        }
    }
    buildTypes {
        getByName("debug") { signingConfig = signingConfigs.getByName("foco") }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { jvmToolchain(17) }
