
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")

}

android {
    namespace = "com.example.anonymousChat"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.anonymousChat"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        renderscriptTargetApi = 18
        renderscriptSupportModeEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures{
        viewBinding = true
        buildConfig = true
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildToolsVersion = "34.0.0"
    androidResources {
        noCompress += listOf("tflite")
    }

}

dependencies {

    implementation ("com.microsoft.signalr:signalr:6.0.0")
    implementation ("com.mikhaellopez:circularimageview:4.3.1")
    implementation ("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.google.firebase:firebase-ml-vision:24.1.0")
    implementation ("com.google.android.gms:play-services-vision:20.1.3")
    implementation("com.google.firebase:firebase-ml-vision-automl:18.0.6")
    implementation("com.google.firebase:firebase-ml-model-interpreter:22.0.4")
    implementation ("com.github.nipunru:nsfw-detector-android:1.0.0")
    implementation ("com.airbnb.android:lottie:6.2.0")
    implementation ("com.github.bumptech.glide:glide:4.12.0")
    implementation ("com.squareup.picasso:picasso:2.8")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation ("org.mindrot:jbcrypt:0.4")
    implementation ("com.google.android.gms:play-services-location:18.0.0")
    implementation ("com.google.firebase:firebase-messaging:23.0.0")
    implementation(platform("com.google.firebase:firebase-bom:32.4.0"))
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.firebase:firebase-database-ktx:20.2.2")
    implementation("com.google.firebase:firebase-database:20.3.0")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation ("com.google.dagger:dagger:2.9")
    implementation("com.google.firebase:firebase-firestore-ktx:24.9.1")
    implementation ("org.mindrot:jbcrypt:0.4")
    implementation (project(":filterBadwordsLibrary"))
    implementation ("androidx.constraintlayout:constraintlayout:2.0.4")
    implementation("androidx.databinding:databinding-runtime:8.2.0")
    implementation (platform("com.google.firebase:firebase-bom:30.3.0z"))
    implementation("com.google.firebase:firebase-storage-ktx:20.3.0")

    testImplementation("junit:junit:4.13.2")
    implementation ("com.google.firebase:firebase-analytics")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation ("com.google.android.material:material:1.9.0")
    implementation("commons-codec:commons-codec:1.14")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.12.0")
    implementation ("com.mikhaellopez:circularimageview:4.3.1")
    implementation ("de.hdodenhof:circleimageview:3.1.0")
    implementation ("com.airbnb.android:lottie:6.2.0")

    //viewmodel
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")

}