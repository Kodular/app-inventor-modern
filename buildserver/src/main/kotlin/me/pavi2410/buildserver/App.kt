package me.pavi2410.buildserver

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.cli.*

fun main(args: Array<String>) {
	val parser = ArgParser("buildserver")

	val shutdownToken by parser.option(ArgType.String, description = "Token needed to shutdown the server remotely.")
	val childProcessRamMb by parser.option(ArgType.Int, description = "Maximum ram that can be used by a child processes, in MB.").default(2048)
	val maxSimultaneousBuilds by parser.option(ArgType.Int, description = "Maximum number of builds that can run in parallel. O means unlimited.").default(0)
	val port by parser.option(ArgType.Int, description = "The port number to bind to on the local machine.").default(9990)
	val requiredHosts by parser.option(ArgType.String, description = "If specified, a list of hosts which are permitted to use this BuildServer, other the server is open to all.").multiple()
	val debug by parser.option(ArgType.Boolean, description = "Turn on debugging, which enables the non-async calls of the buildserver.").default(false)
	val dexCacheDir by parser.option(ArgType.String, description = "the directory to cache the pre-dexed libraries")

	parser.parse(args)

	embeddedServer(Netty, port = 8000) {
		configureRouting()
	}.start(wait = true)
}