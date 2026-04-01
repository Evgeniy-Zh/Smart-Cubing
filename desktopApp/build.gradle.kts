import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version ("2.1.0")
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.compose") version "1.10.3"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

dependencies {
    implementation(project(":bleWindows"))
    implementation(project(":bleApi"))
    implementation(project(":cubing"))
    implementation(compose.desktop.currentOs)
    implementation(libs.koin)

    implementation("io.insert-koin:koin-core-viewmodel:4.0.0")
    implementation("io.insert-koin:koin-compose-viewmodel:4.0.0")

}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "DesctopCompose"
            packageVersion = "1.0.0"
        }


        dependencies {
//            implementation(libs.androidx.navigation.runtime.desktop)
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0-RC")

        }
    }

}