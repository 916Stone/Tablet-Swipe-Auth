plugins {
    alias(libs.plugins.androidApplication)
    id("com.google.gms.google-services")
//    id("com.chaquo.python")
}

android {
    namespace = "com.example.tablet"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.tablet"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("x86_64", "arm64-v8a", "x86")
        }
    }

//    flavorDimensions += "pyVersion"
//    productFlavors {
//        create("py310") { dimension = "pyVersion" }
//        create("py311") { dimension = "pyVersion" }
//    }

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
    buildFeatures {
        viewBinding = true
    }
}

//chaquopy {
//    defaultConfig {
//        //C:\Users\Stone\anaconda3
//        buildPython("C:/Users/Stone/anaconda3/python.exe")
//    }
//    productFlavors {
//        getByName("py310") { version = "3.10" }
//        getByName("py311") { version = "3.11" }
//    }
//}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("org.apache.commons:commons-math3:3.0")
    implementation(files("libs/isolationForest.jar"))
    implementation(files("libs/weka-stable-3.8.1.jar"))
}
