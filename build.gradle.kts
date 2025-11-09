// Top-level build file

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // CORRIGIDO: Usa a sintaxe de função classpath(...)
        classpath("com.android.tools.build:gradle:8.4.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.23")
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
