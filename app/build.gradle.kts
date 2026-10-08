import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.rutaalacima.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rutaalacima.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "0.6.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Servidor (Supabase): se leen de local.properties para no subir claves al repositorio.
        //   supabase.url=https://xxxx.supabase.co
        //   supabase.anonKey=eyJ...
        // Si están vacíos, la app funciona en modo demo (todo local).
        val props = Properties().apply {
            val f = rootProject.file("local.properties")
            if (f.exists()) f.inputStream().use { load(it) }
        }
        buildConfigField("String", "SUPABASE_URL", "\"${props.getProperty("supabase.url", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${props.getProperty("supabase.anonKey", "")}\"")
        // Dirección de Rutaalacima Web (carpeta /web publicada con GitHub Pages u otro hosting)
        buildConfigField("String", "WEB_URL", "\"${props.getProperty("rutacima.webUrl", "https://passbri.github.io/rutacima/")}\"")
    }

    // Firma de publicación (Google Play): keystore.properties en la raíz del proyecto, nunca en el repositorio.
    //   storeFile=/ruta/a/rutaalacima.jks
    //   storePassword=…
    //   keyAlias=rutaalacima
    //   keyPassword=…
    // En GitHub Actions se arma desde los secretos RUTACIMA_KEYSTORE_BASE64, RUTACIMA_KEYSTORE_PASSWORD, RUTACIMA_KEY_ALIAS y RUTACIMA_KEY_PASSWORD.
    val firma = Properties().apply {
        val f = rootProject.file("keystore.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    signingConfigs {
        if (firma.getProperty("storeFile") != null) {
            create("publicacion") {
                storeFile = file(firma.getProperty("storeFile"))
                storePassword = firma.getProperty("storePassword")
                keyAlias = firma.getProperty("keyAlias")
                keyPassword = firma.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Código y recursos reducidos: app más liviana y más difícil de copiar
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("publicacion")?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    androidResources {
        // Idiomas de la interfaz (selector por app en Android 13+ y en Ajustes de la app).
        generateLocaleConfig = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    sourceSets {
        // Los tests JVM leen el contenido real de assets para validar el JSON.
        getByName("test").resources.srcDir("src/main/assets")
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.biometric)
    implementation(libs.play.services.code.scanner)
    // Widget de la pantalla de inicio (frase del día, hábitos y racha)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    testImplementation(libs.junit)
    // Pruebas de pantallas en la JVM (sin emulador)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
