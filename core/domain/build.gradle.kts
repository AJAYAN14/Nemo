plugins {
    id("java-library")
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Core Modules
    api(project(":core:common"))
    
    // Serialization
    api(libs.kotlinx.serialization.json)
    
    // Kotlin
    implementation(libs.kotlinx.coroutines.core)
    
    // Dependency Injection
    implementation(libs.javax.inject)

    
    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
