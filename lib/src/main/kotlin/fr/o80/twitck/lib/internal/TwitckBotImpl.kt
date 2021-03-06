package fr.o80.twitck.lib.internal

import fr.o80.twitck.lib.api.TwitckBot
import fr.o80.twitck.lib.api.bean.TwitckConfiguration
import fr.o80.twitck.lib.api.service.log.Logger
import fr.o80.twitck.lib.internal.handler.AutoJoiner
import fr.o80.twitck.lib.internal.handler.JoinDispatcher
import fr.o80.twitck.lib.internal.handler.MessageDispatcher
import fr.o80.twitck.lib.internal.handler.WhisperDispatcher
import fr.o80.twitck.lib.internal.service.Ping
import fr.o80.twitck.lib.internal.service.line.JoinLineHandler
import fr.o80.twitck.lib.internal.service.line.LineInterpreter
import fr.o80.twitck.lib.internal.service.line.PrivMsgLineHandler
import fr.o80.twitck.lib.internal.service.line.WhisperLineHandler
import org.jibble.pircbot.PircBot

internal class TwitckBotImpl(
    private val configuration: TwitckConfiguration
) : PircBot(), TwitckBot {

    private val initializer = TwitckInitializer()

    private val ping = Ping(this)

    private val privMsgLineHandler = PrivMsgLineHandler(this, MessageDispatcher(this, configuration.messageHandlers))
    private val joinLineHandler = JoinLineHandler(this, JoinDispatcher(this, configuration.joinHandlers))
    private val whisperLineHandler = WhisperLineHandler(this, WhisperDispatcher(this, configuration.whisperHandlers))

    private val autoJoiner = AutoJoiner(this, configuration.requestedChannels, configuration.loggerFactory)

    private val lineInterpreter = LineInterpreter(privMsgLineHandler, joinLineHandler, whisperLineHandler)

    private val logger: Logger = configuration.loggerFactory.getLogger(TwitckBotImpl::class)

    override fun connectToServer() {
        logger.info("Attempting to connect to irc.twitch.tv...")

        connect(HOST, PORT, "oauth:${configuration.oauthToken}")

        logger.info("Requesting twitch membership capability for NAMES/JOIN/PART/MODE messages...")
        sendRawLine(SERVER_MEMREQ)

        logger.info("Requesting twitch commands capability for NOTICE/HOSTTARGET/CLEARCHAT/USERSTATE messages... ")
        sendRawLine(SERVER_CMDREQ)

        logger.info("Requesting twitch tags capability for PRIVMSG/USERSTATE/GLOBALUSERSTATE messages... ")
        sendRawLine(SERVER_TAGREG)

        while (!initializer.initialized) {
            Thread.sleep(1000)
            if (!initializer.initialized) logger.debug("Not yet initialized")
        }

        autoJoiner.join()
    }

    override fun join(channel: String) {
        logger.info("Attempting to join channel $channel")
        super.joinChannel(channel)
    }

    override fun handleLine(line: String?) {
        // TODO OPZ Verbose ?
        logger.debug("Handle line: $line")
        super.handleLine(line)
        line ?: return

        initializer.handleLine(line)
        ping.handleLine(line)
        lineInterpreter.handle(line)
    }

    override fun sendLine(line: String) {
        sendRawLine(line)
    }

    override fun send(channel: String, message: String) {
        sendMessage(channel, message)
    }

    override fun onMessage(
        channel: String,
        sender: String,
        login: String,
        hostname: String,
        message: String
    ) {
        super.onMessage(channel, sender, login, hostname, message)
        logger.debug("Message received from $sender on channel $channel: $message")
    }

    override fun onJoin(
        channel: String,
        sender: String,
        login: String,
        hostname: String
    ) {
        super.onJoin(channel, sender, login, hostname)
        logger.debug("$sender join the channel $channel")
    }
}