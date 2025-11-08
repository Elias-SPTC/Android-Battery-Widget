// Arquivo: build.gradle.kts (Nível do Projeto/Raiz)

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // CORREÇÃO: Atualização do Android Gradle Plugin (8.1.1 -> 8.4.1)
        classpath("com.android.tools.build:gradle:8.4.1")

        // CORREÇÃO: Atualização do Kotlin Gradle Plugin (1.9.0 -> 1.9.23)
        // Nota: O plugin do Kotlin é frequentemente declarado no bloco plugins{} no settings.gradle.
        // Se este bloco for necessário, a sintaxe é essa:
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.23")
    }
}

// O bloco allprojects é geralmente substituído por configurations no settings.gradle.
// Não é necessário neste arquivo, conforme sua observação anterior.

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}