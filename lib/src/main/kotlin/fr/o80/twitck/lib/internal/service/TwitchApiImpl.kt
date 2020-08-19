package fr.o80.twitck.lib.internal.service

import com.google.gson.GsonBuilder
import fr.o80.twitck.lib.api.bean.Channel
import fr.o80.twitck.lib.api.bean.Follower
import fr.o80.twitck.lib.api.bean.User
import fr.o80.twitck.lib.api.service.TwitchApi
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class TwitchApiImpl(
    private val clientId: String,
    private val oauthToken: String
) : TwitchApi {

    private val gson = GsonBuilder().create()
    private val client = HttpClient.newHttpClient()

    override fun getFollowers(streamId: String): List<Follower> {
        val url = "https://api.twitch.tv/kraken/channels/$streamId/follows"
        val answer = doRequest(url).parse<FollowAnswer>()
        return answer.follows
    }

    override fun getUser(userName: String): User {
        val url = "https://api.twitch.tv/kraken/users?login=$userName"
        val answer = doRequest(url).parse<UserAnswer>()
        return answer.users[0]
    }

    override fun getChannel(channelId: String): Channel {
        val url = "https://api.twitch.tv/kraken/channels/$channelId"
        return doRequest(url).parse()
    }

    private fun doRequest(url: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Client-ID", clientId)
            .header("Authorization", "OAuth $oauthToken")
            .header("Accept", "application/vnd.twitchtv.v5+json")
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    private inline fun <reified T> String.parse(): T {
        return gson.fromJson(this, T::class.java)
    }
}


class FollowAnswer(
    val follows: List<Follower>
)

class UserAnswer(
    val users: List<User>
)