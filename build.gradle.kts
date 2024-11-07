plugins {
    alias(libs.plugins.android.application) apply false
    // Use the version from your Version Catalog
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    }

buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.3.15")
    }
}

