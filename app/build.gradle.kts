plugins {
    id("com.android.application")
}

android {
    namespace = "com.ai.aiscriptmurde"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ai.aiscriptmurde"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")


    // 核心库
    implementation("androidx.room:room-runtime:2.5.2")
    // 编译器 (Java 专用)
    annotationProcessor("androidx.room:room-compiler:2.5.2")
    // 网络请求库
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    // Retrofit 网络请求库 (基于 OkHttp 封装，通过接口定义 API)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    // Retrofit Gson 转换器 (配合已有的 Gson，自动将 JSON 转为 Java 对象)
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Glide 图片加载库 (显示图片)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

}