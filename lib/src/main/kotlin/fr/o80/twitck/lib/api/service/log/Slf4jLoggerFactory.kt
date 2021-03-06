package fr.o80.twitck.lib.api.service.log

import fr.o80.twitck.lib.api.bean.Command
import kotlin.reflect.KClass

class Slf4jLoggerFactory : LoggerFactory {
    override fun getLogger(klass: KClass<*>): Logger =
        Slf4jLoggerAdapter(klass)
}

private class Slf4jLoggerAdapter(klass: KClass<*>) : Logger {

    private val slf4jLogger: org.slf4j.Logger =
        org.slf4j.LoggerFactory.getLogger(klass.java)

    override fun command(command: Command, message: String) {
        slf4jLogger.debug("[Command:${command.tag}] $message")
    }

    override fun info(message: String) {
        slf4jLogger.info(message)
    }

    override fun debug(message: String) {
        slf4jLogger.debug(message)
    }
}