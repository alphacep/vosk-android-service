plugins {
    id("com.android.application")
}

repositories {
    google()
    maven("https://alphacephei.com/maven/")
}

android {
    compileSdk = 33
    defaultConfig {
        applicationId = "org.vosk.service"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.1"
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86_64", "x86")
        }
        splits {
            abi {
                isEnable = true

                isUniversalApk = true
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
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
}

dependencies {
    implementation("com.alphacephei:vosk-android:0.3.46@aar")
    implementation("net.java.dev.jna:jna:5.13.0@aar")
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.google.android.material:material:1.6.1")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("io.reactivex.rxjava2:rxjava:2.2.9")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:adapter-rxjava2:2.3.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.github.pwittchen:reactivenetwork-rx2:0.12.3")
    implementation("commons-io:commons-io:2.11.0")
}
