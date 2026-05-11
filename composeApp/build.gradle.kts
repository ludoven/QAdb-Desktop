import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@CacheableTask
abstract class GenerateAppVersionTask : DefaultTask() {
    @get:Input
    abstract val versionName: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package com.ludoven.adbtool

            object AppVersion {
                const val CURRENT = "${versionName.get()}"
            }
            """.trimIndent()
        )
    }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

val appVersion = "2.0.3"
val generatedVersionSourceDir = layout.buildDirectory.dir("generated/source/appVersion/desktopMain/kotlin")
val generateDesktopAppVersion = tasks.register<GenerateAppVersionTask>("generateDesktopAppVersion") {
    versionName.set(appVersion)
    outputFile.set(generatedVersionSourceDir.map { it.file("com/ludoven/adbtool/AppVersion.kt") })
}

kotlin {
    jvm("desktop")
    
    sourceSets {
        val desktopMain by getting {
            kotlin.srcDir(generatedVersionSourceDir)
        }
        
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(compose.materialIconsExtended)
            // JetBrains Compose Multiplatform 的 ViewModel 支持
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
            implementation("org.jetbrains.androidx.navigation:navigation-compose:2.9.0-beta03")


            val fileKit = "0.10.0"
            implementation("io.github.vinceglb:filekit-core:$fileKit")
            implementation("io.github.vinceglb:filekit-dialogs:$fileKit")
            implementation("io.github.vinceglb:filekit-dialogs-compose:$fileKit")
            implementation("io.github.vinceglb:filekit-coil:$fileKit")
            implementation("net.java.dev.jna:jna:5.18.1")
            implementation("net.java.dev.jna:jna-platform:5.18.1")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        desktopMain.dependencies {
            implementation(enforcedPlatform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.8.0"))
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}


compose.desktop {
    application {
        mainClass = "com.ludoven.adbtool.MainKt"
        jvmArgs += listOf("-Djna.nosys=true")

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe)

            packageVersion = appVersion
            packageName = "QADB"

//            iconFile.set(project.file("src/desktopMain/composeResources/icons/app_icon.icns")) // macOS 图标
            windows.iconFile.set(project.file("src/desktopMain/composeResources/drawable/app_icon.ico")) // Windows 图标
            windows {
                // Ensure installers create desktop/start-menu entries by default on Windows.
                shortcut = true
                menu = true
                menuGroup = "QADB"
            }

            macOS {
                bundleID = "com.ludoven.adbtool"
                iconFile.set(project.file("src/desktopMain/composeResources/drawable/app_icon.icns")) // macOS 图标
            }
        }
    }
}
tasks.withType<KotlinCompile>().configureEach {
    dependsOn(generateDesktopAppVersion)
    compilerOptions {
        freeCompilerArgs.add("-Xnon-local-break-continue")
    }
}
