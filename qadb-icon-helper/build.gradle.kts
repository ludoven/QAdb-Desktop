import org.gradle.api.GradleException
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar
import org.gradle.process.ExecOperations
import java.util.Properties
import javax.inject.Inject

abstract class AssembleIconHelperDexTask @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {
    @get:InputFile
    abstract val inputJar: RegularFileProperty

    @get:InputFile
    abstract val d8Executable: RegularFileProperty

    @get:OutputFile
    abstract val outputJar: RegularFileProperty

    @TaskAction
    fun assemble() {
        val output = outputJar.get().asFile
        output.parentFile.mkdirs()
        execOperations.exec {
            commandLine(
                d8Executable.get().asFile.absolutePath,
                "--min-api",
                "24",
                "--output",
                output.absolutePath,
                inputJar.get().asFile.absolutePath
            )
        }
    }
}

plugins {
    `java-library`
}

group = "com.ludoven.qadb"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

fun androidSdkDir(): File {
    val localProperties = rootProject.file("local.properties")
    val localSdk = if (localProperties.exists()) {
        Properties().apply {
            localProperties.inputStream().use { load(it) }
        }.getProperty("sdk.dir")
    } else {
        null
    }
    val sdkPath = localSdk
        ?: System.getenv("ANDROID_HOME")
        ?: System.getenv("ANDROID_SDK_ROOT")
        ?: throw GradleException("Android SDK not found. Set sdk.dir in local.properties or ANDROID_HOME.")
    return file(sdkPath)
}

fun latestAndroidJar(): File {
    val platformsDir = androidSdkDir().resolve("platforms")
    val platform = platformsDir.listFiles()
        ?.filter { it.isDirectory && it.name.startsWith("android-") }
        ?.maxByOrNull { it.name.removePrefix("android-").toIntOrNull() ?: 0 }
        ?: throw GradleException("No Android platforms found under ${platformsDir.absolutePath}")
    return platform.resolve("android.jar").takeIf { it.isFile }
        ?: throw GradleException("android.jar not found under ${platform.absolutePath}")
}

fun latestD8(): File {
    val buildToolsDir = androidSdkDir().resolve("build-tools")
    val executableName = if (System.getProperty("os.name").lowercase().contains("windows")) "d8.bat" else "d8"
    return buildToolsDir.listFiles()
        ?.filter { it.isDirectory }
        ?.sortedByDescending { it.name }
        ?.map { it.resolve(executableName) }
        ?.firstOrNull { it.isFile && it.canExecute() }
        ?: throw GradleException("d8 not found under ${buildToolsDir.absolutePath}")
}

dependencies {
    compileOnly(files(latestAndroidJar()))
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
}

val helperClassesJar = tasks.named<Jar>("jar")
val assembleIconHelperDex = tasks.register<AssembleIconHelperDexTask>("assembleIconHelperDex") {
    group = "build"
    description = "Builds the app_process-compatible dex jar for QADB icon helper."
    dependsOn(helperClassesJar)

    val dexOutputJar = layout.buildDirectory.file("outputs/qadb-icon-helper.jar")
    inputJar.set(helperClassesJar.flatMap { it.archiveFile })
    d8Executable.set(file(latestD8().absolutePath))
    outputJar.set(dexOutputJar)
    outputs.file(dexOutputJar)
}

tasks.named("assemble") {
    dependsOn(assembleIconHelperDex)
}
