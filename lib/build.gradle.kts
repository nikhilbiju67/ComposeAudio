import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("com.vanniktech.maven.publish") version "0.30.0"
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    jvm("desktop")
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "library"
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        // Serve sources to debug inside browser
                        add(rootDirPath)
                        add(projectDirPath)
                    }
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.media3.exoplayer)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.vlcj) // vlcj library
            implementation(libs.vlcj.natives) // Native libraries for vlcj
        }
        wasmJsMain.dependencies {
            implementation(libs.kotlinx.browser)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

android {
    namespace = "com.nikhilbiju67.audio"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {

    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}
// 1. Set the project’s version at the very top of this Gradle file:
val resolvedVersion = project.findProperty("ORG_GRADLE_PROJECT_VERSION_NAME")
    ?: "1.0.3-SNAPSHOT"

project.version = resolvedVersion // Must happen *before* configuring mavenPublishing
group = "com.nikhilbiju67.audio"
mavenPublishing {
//    publishToMavenCentral(SonatypeHost.DEFAULT)
//    // or when publishing to https://s01.oss.sonatype.org
//    publishToMavenCentral(SonatypeHost.S01)
//    // or when publishing to https://central.sonatype.com/
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()


    coordinates("com.nikhilbiju67.audio", "nikhilbiju67-compose-audio-runtime")

    pom {
        name.set("Compose Audio")
        description.set("A description of what my library does.")
        inceptionYear.set("2025")
        url.set("https://github.com/nikhilbiju67/ComposeAudio")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("nikhilbiju67")
                name.set("Nikhilbiju67")
                url.set("https://github.com/nikhilbiju67")
            }
        }
        scm {
            url.set("https://github.com/nikhilbiju67/ComposeAudio")
            connection.set("scm:git:git://github.com/nikhilbiju67/ComposeAudio.git")
            developerConnection.set("scm:git:ssh://git@github.com/nikhilbiju67/ComposeAudio.git")

        }
        issueManagement {
            system = "GitHub"
            url = "https://github.com/nikhilbiju67/${project.name}/issues"
        }
        ciManagement {
            system = "GitHub Actions"
            url = "https://github.com/nikhilbiju67/${project.name}/actions"
        }
    }
}