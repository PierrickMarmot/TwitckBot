package fr.o80.twitck.extension

import fr.o80.twitck.lib.api.Pipeline
import fr.o80.twitck.lib.api.bean.Command
import fr.o80.twitck.lib.api.bean.MessageEvent
import fr.o80.twitck.lib.api.TwitckBot
import fr.o80.twitck.lib.api.extension.HelperExtension
import fr.o80.twitck.lib.api.extension.TwitckExtension
import fr.o80.twitck.lib.api.service.CommandParser
import fr.o80.twitck.lib.api.service.ServiceLocator

class Help(
    private val channel: String,
    private val registeredCommands: MutableMap<String, String?>,
    private val commandParser: CommandParser
) : HelperExtension {

    fun interceptMessageEvent(bot: TwitckBot, messageEvent: MessageEvent): MessageEvent {
        if (channel != messageEvent.channel)
            return messageEvent

        commandParser.parse(messageEvent)?.let { command ->
            reactToCommand(command, bot, messageEvent)
        }

        return messageEvent
    }

    override fun registerCommand(command: String) {
        registeredCommands[command] = null
    }

    private fun reactToCommand(
        command: Command,
        bot: TwitckBot,
        messageEvent: MessageEvent
    ) {
        when (command.tag) {
            "!help" -> {
                bot.sendHelp(messageEvent.channel, registeredCommands.keys)
            }
            in registeredCommands.keys -> {
                registeredCommands[command.tag]?.let { message ->
                    bot.send(messageEvent.channel, message)
                }
            }
        }
    }

    private fun TwitckBot.sendHelp(
        channel: String,
        commands: Collection<String>
    ) {
        if (commands.isEmpty()) {
            this.send(channel, "Je ne sais rien faire O_o du moins pour l'instant...")
        } else {
            val commandsExamples = commands.joinToString(", ")
            this.send(channel, "Je sais faire un paquet de choses, par exemple : $commandsExamples")
        }
    }

    class Configuration {

        @DslMarker
        private annotation class HelpDsl

        private var channel: String? = null
        private var registeredCommands = mutableMapOf<String, String?>()

        @HelpDsl
        fun channel(channel: String) {
            this.channel = channel
        }

        @HelpDsl
        fun registerCommand(command: String, message: String? = null) {
            registeredCommands[command] = message
        }

        fun build(serviceLocator: ServiceLocator): Help {
            val channelName = channel
                ?: throw IllegalStateException("Channel must be set for the extension ${Help::class.simpleName}")

            return Help(channelName, registeredCommands, serviceLocator.commandParser)
        }
    }

    companion object Extension : TwitckExtension<Configuration, Help> {
        override fun install(
            pipeline: Pipeline,
            serviceLocator: ServiceLocator,
            configure: Configuration.() -> Unit
        ): Help {
            return Configuration()
                .apply(configure)
                .build(serviceLocator)
                .also { localHelp ->
                    pipeline.interceptMessageEvent(localHelp::interceptMessageEvent)
                }
        }
    }


}
