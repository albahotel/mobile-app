// import org.jetbrains.kotlin.gradle.idea.proto.com.google.protobuf.option

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.protobuf)
}

android {
    namespace = "dev.rexe.hotelremote"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.rexe.hotelremote"
        minSdk = 21
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.okhttp)
    implementation(libs.protobuf.kotlin.lite)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.legacy.support.v4)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

protobuf {
    protoc {
        artifact = if (osdetector.os == "osx") {
            // support both Apple Silicon and Intel chipsets
            val arch = System.getProperty("os.arch")
            val suffix = if (arch == "x86_64") "x86_64" else "aarch_64"
            "${libs.protobuf.protoc.get()}:osx-$suffix"
        } else
            libs.protobuf.protoc.get().toString()
    }
    plugins {
        generateProtoTasks {
            all().forEach {
                it.builtins {
                    create("java") {
                        option("lite")
                    }
                }
            }
        }
    }
}