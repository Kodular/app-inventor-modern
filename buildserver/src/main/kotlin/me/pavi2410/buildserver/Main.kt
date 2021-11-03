package me.pavi2410.buildserver

import com.google.appinventor.buildserver.ProjectBuilder
import com.google.appinventor.buildserver.Result
import kotlinx.cli.*
import java.io.File
import java.io.IOException
import java.util.logging.Logger
import java.util.zip.ZipFile

private val LOG = Logger.getLogger("Buildserver CLI")

fun main(args: Array<String>) {
    val parser = ArgParser("cli")

    val isForCompanion by parser.option(ArgType.Boolean, description = "create the MIT AI2 Companion APK").default(false)
    val inputZipFile by parser.option(ArgType.String, description = "the ZIP file of the project to build").required()
    val userName by parser.option(ArgType.String, description = "the name of the user building the project").required()
    val outputDir by parser.option(ArgType.String, description = "the directory in which to put the output of the build").required()
    val childProcessRamMb by parser.option(ArgType.Int, description = "Maximum ram that can be used by a child processes, in MB.").default(2048)
    val dexCacheDir by parser.option(ArgType.String, description = "the directory to cache the pre-dexed libraries")
    val includeDangerousPermissions by parser.option(ArgType.Boolean, description = "Add extra features not allowed in the Google Play store.").default(false)
    val extensions by parser.option(ArgType.String, description = "Include the named extensions in the compilation.").multiple()
    val outputFileName by parser.option(ArgType.String, description = "Use the specified file name for output rather than the App Name.")
    val isForEmulator by parser.option(ArgType.Boolean, description = "Exclude native libraries for emulator.").default(false)
    val ext by parser.option(ArgType.String, description = "Specifies the build type to use.").default("apk")

    parser.parse(args)

    val inputZip: ZipFile = try {
        ZipFile(inputZipFile)
    } catch (e: IOException) {
        LOG.severe("Problem opening inout zip file: $inputZipFile")
        kotlin.system.exitProcess(1)
    }

    val projectBuilder = ProjectBuilder()
    val result: Result = projectBuilder.build(
        userName,
        inputZip,
        File(outputDir),
        outputFileName,
        isForCompanion,
        isForEmulator,
        includeDangerousPermissions,
        extensions,
        childProcessRamMb,
        dexCacheDir,
        null,
        constants.AAB_EXTENSION_VALUE == ext
    )
    kotlin.system.exitProcess(result.result)
}