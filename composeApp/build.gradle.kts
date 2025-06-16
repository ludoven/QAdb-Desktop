import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    jvm("desktop")
    
    sourceSets {
        val desktopMain by getting
        
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(compose.materialIconsExtended)
            // JetBrains Compose Multiplatform 的 ViewModel 支持
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")


            implementation("org.openjfx:javafx-base:21:win")     // 根据平台选择
            implementation("org.openjfx:javafx-graphics:21:win")
            implementation("org.openjfx:javafx-controls:21:win")
            implementation("org.openjfx:javafx-base:21:mac")     // 根据平台选择
            implementation("org.openjfx:javafx-graphics:21:mac")
            implementation("org.openjfx:javafx-controls:21:mac")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}


compose.desktop {
    application {
        mainClass = "com.ludoven.adbtool.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe)

            packageVersion = "1.0.0"
            packageName = "QAdb"

            macOS {
                bundleID = "com.ludoven.adbtool" // Bundle Identifier
                iconFile.set(project.file("desktopMain/composeResources/drawable/img.png"))
            }
        }
    }
}
