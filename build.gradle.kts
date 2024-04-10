val kotlinVersion: String = "1.9.23"

plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.23"

    id("com.diffplug.spotless") version "6.19.0"
}

group = "org.rasteplads"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
 //   implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1-Beta")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.9")
    implementation(kotlin("reflect"))

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}

spotless {
    kotlin {
        ktfmt("0.44").dropboxStyle()
    }
}
