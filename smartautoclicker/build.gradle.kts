/*
 * Copyright (C) 2025 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") version "2.1.0"
    id("dagger.hilt.android.plugin")
    id("kotlin-kapt")
}

android {
    namespace = "com.buzbuz.smartautoclicker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.buzbuz.smartautoclicker"
        minSdk = 23
        targetSdk = 34
        versionCode = 65
        versionName = "3.3.0-beta04"
        multiDexEnabled = true
    }

    // ✅ Fix packaging issues for TensorFlow Lite
    packagingOptions {
        jniLibs {
            pickFirsts.add("lib/**/libtensorflowlite.so")
            pickFirsts.add("lib/**/libtensorflowlite_jni.so")
            keepDebugSymbols.add("**/*.so")
        }
        resources {
            excludes.add("META-INF/**")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // ✅ Core AndroidX Libraries
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.7.0")
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // ✅ UI & Material Components
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.airbnb.android:lottie:6.0.0")

    // ✅ TensorFlow Lite Dependencies (Optimized & Fixed)
    implementation("org.tensorflow:tensorflow-lite:2.16.0")
    implementation("org.tensorflow:tensorflow-lite-support:2.16.0")
    implementation("org.tensorflow:tensorflow-lite-metadata:2.16.0")
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.16.0")
    implementation("org.tensorflow:tensorflow-lite-delegates-gpu:2.16.0")
    implementation("org.tensorflow:tensorflow-lite-delegates-nnapi:2.16.0")

    // ✅ Kotlin Coroutines for Async Processing
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")

    // ✅ Dagger Hilt (Dependency Injection)
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")

    // ✅ Required Project Modules
    implementation(project(":core:common:base"))
    implementation(project(":core:common:bitmaps"))
    implementation(project(":core:common:display"))
    implementation(project(":core:common:overlays"))
    implementation(project(":core:common:permissions"))
    implementation(project(":core:common:quality"))
    implementation(project(":core:common:settings"))
    implementation(project(":core:common:ui"))
    implementation(project(":core:dumb"))
    implementation(project(":core:smart:detection"))
    implementation(project(":core:smart:domain"))
    implementation(project(":core:smart:processing"))
    implementation(project(":feature:backup"))
    implementation(project(":feature:notifications"))
    implementation(project(":feature:quick-settings-tile"))
    implementation(project(":feature:revenue"))
    implementation(project(":feature:review"))
    implementation(project(":feature:smart-config"))
    implementation(project(":feature:smart-debugging"))
    implementation(project(":feature:dumb-config"))
    implementation(project(":feature:tutorial"))
}
