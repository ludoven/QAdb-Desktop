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
            implementation("org.jetbrains.androidx.navigation:navigation-compose:2.9.0-beta03")


            val fileKit = "0.10.0-beta04"
            implementation("io.github.vinceglb:filekit-core:$fileKit")
            implementation("io.github.vinceglb:filekit-dialogs:$fileKit")
            implementation("io.github.vinceglb:filekit-dialogs-compose:$fileKit")
            implementation("io.github.vinceglb:filekit-coil:$fileKit")

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

            packageVersion = "1.0.1"
            packageName = "QAdb"

//            iconFile.set(project.file("src/desktopMain/composeResources/icons/app_icon.icns")) // macOS 图标
            windows.iconFile.set(project.file("src/desktopMain/composeResources/drawable/app_icon.ico")) // Windows 图标

            macOS {
                bundleID = "com.ludoven.adbtool"
                iconFile.set(project.file("src/desktopMain/composeResources/drawable/app_icon.icns")) // macOS 图标
            }
        }
    }
}
