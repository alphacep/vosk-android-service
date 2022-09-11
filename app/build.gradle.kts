plugins {
	id("com.android.application")
}

repositories {
	google()
	maven("https://alphacephei.com/maven/")
}

android {
	compileSdk = 32
	defaultConfig {
		applicationId = "org.vosk.service"
		minSdk = 24
		targetSdk = 32
		versionCode = 1
		versionName = "1.1"
		ndk {
			abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86_64", "x86")
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
	implementation("androidx.appcompat:appcompat:1.5.1")
	implementation("com.google.code.gson:gson:2.9.0")
	implementation("com.google.android.material:material:1.6.1")
	implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
	implementation("io.reactivex.rxjava2:rxjava:2.2.9")
	implementation("com.squareup.retrofit2:retrofit:2.9.0")
	implementation("com.squareup.retrofit2:converter-gson:2.9.0")
	implementation("com.squareup.retrofit2:adapter-rxjava2:2.3.0")
	implementation("com.alphacephei:vosk-android:0.3.38")
	implementation("com.alphacephei:vosk-model-en:0.3.38")
	implementation("com.google.firebase:firebase-crashlytics-buildtools:2.9.1")
	implementation("androidx.constraintlayout:constraintlayout:2.1.4")
	implementation("com.github.pwittchen:reactivenetwork-rx2:0.12.3")
	implementation("commons-io:commons-io:2.11.0")
}
