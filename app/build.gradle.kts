// Arquivo: app/build.gradle.kts (Nível do Módulo)

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // CORREÇÃO: Atualiza a versão do KSP para corresponder à do Kotlin
    id("com.google.devtools.ksp") version "1.9.23-1.0.19"
    id("kotlin-parcelize")
}

// Definição das versões das bibliotecas
val koinVersion = "3.5.6"
// CORREÇÃO: Atualiza a versão do Room para resolver o erro de compilação do KSP
val roomVersion = "2.6.1"
val lifecycleVersion = "2.8.0-rc01"
val navVersion = "2.7.5"
val datastoreVersion = "1.0.0"

android {
    namespace = "com.em.batterywidget"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.em.batterywidget"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Adiciona a configuração de schema para o KSP, resolvendo o aviso do Room
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
}

dependencies {
    // Dependências básicas
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.preference:preference-ktx:1.2.1")

    // --- Injeção de Dependência: KOIN ---
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-android:$koinVersion")
    implementation("io.insert-koin:koin-androidx-workmanager:$koinVersion")

    // --- Persistência de Dados: ROOM ---
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // --- Componentes de Arquitetura ---
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")

    // Navigation Components
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Gráficos
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:$datastoreVersion")

    // Testes
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
