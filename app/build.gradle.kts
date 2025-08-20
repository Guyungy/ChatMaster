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
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    signingConfigs {
        create("release") {
            storeFile = file("keystore.jks") // 注意：文件名需与 workflow 中一致
            storePassword= System.getenv("STORE_PASSWORD")
            keyAlias= System.getenv("KEY_ALIAS")
            keyPassword= System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        // ✅ 无需显式配置 debug 签名 - 系统会自动使用默认配置
        debug {

        }

        release {
            signingConfig = signingConfigs.getByName("release")

            isMinifyEnabled = false
//            isMinifyEnabled = true // 不想写proguard-rules.pro
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }

    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.ApkVariantOutputImpl }
            .forEach { output ->
                val versionName = variant.versionName ?: "unknown"
                val buildType = variant.buildType.name
                val appName = rootProject.name // <-- 请替换为您的实际应用名称或项目名称
                val newApkName = StringBuilder()
                    .append(appName)
                    .append("-v")
                    .append(versionName)
                    .append("-")
                    .append(buildType)
                    .append(".apk")
                    .toString()

                // 设置新的 APK 文件名
                output.outputFileName = newApkName
            }
    }

}

dependencies {
    implementation("androidx.compose.material3:material3:1.2.1")

    // 无障碍
    implementation("com.github.ven-coder.Assists:assists-base:3.2.180")

    // deepseek
    implementation("cn.lishiyuan:deepseek4j:1.0.1")
    implementation("com.alibaba.fastjson2:fastjson2-kotlin:2.0.58")
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

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

    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}