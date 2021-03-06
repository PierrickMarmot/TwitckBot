package fr.o80.twitck.lib.api.extension

/**
 * Interface to fulfill when you want to implement your own TwitckExtension that repond to !help command.
 */
interface HelperExtension {
    fun registerCommand(command: String)
}