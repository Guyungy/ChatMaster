plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.liganma.chatmaster"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.liganma.chatmaster"
        minSdk = 24
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
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.compose.material3:material3:1.2.1")

    // 无障碍
    //按需添加
    //基础库（必须）
    implementation("com.github.ven-coder.Assists:assists-base:3.2.180")

    implementation("com.squareup.okhttp3:okhttp:5.1.0")

    // 浮窗
    implementation("io.github.petterpx:floatingx:2.3.5")
    // system浮窗&&compose时需要导入
    // 记得AppHelper里调用 enableComposeSupport()
    implementation("io.github.petterpx:floatingx-compose:2.3.5")

    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.0")


    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7") // Fragment 导航
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")      // UI 集成
    implementation("androidx.navigation:navigation-compose:2.7.7")     // Compose 导航 :cite[4]:cite[7]

    implementation("androidx.compose.material:material-icons-extended:1.7.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}