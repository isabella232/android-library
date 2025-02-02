plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
}

dependencies {
    implementation("com.android.tools.build:gradle:7.1.3")
    implementation(kotlin("gradle-plugin", version = "1.5.31"))
    implementation("io.github.gradle-nexus:publish-plugin:1.1.0")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.4.32")
}

group = "build"

gradlePlugin {
    plugins {
        register("airshipPublish") {
            id = "airship-publish"
            implementationClass = "AirshipPublishPlugin"
        }
        register("airshipDokka") {
            id = "airship-dokka"
            implementationClass = "AirshipDokkaPlugin"
        }
        register("airshipDoclava") {
            id = "airship-doclava"
            implementationClass = "AirshipDoclavaPlugin"
        }
        register("airshipModule") {
            id = "airship-module"
            implementationClass = "AirshipModulePlugin"
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
