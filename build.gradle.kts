buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.0")

    }
    repositories {
        mavenCentral()
    }

}


// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {

   kotlin("kapt") version "1.9.20"
    id("com.android.application") version "8.1.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
}
