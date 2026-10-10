package com.kaspa.livewidget.api

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Short, read-only JSON-wRPC session. No subscriptions or wallet calls. */
class PublicNodeFallback(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(6, TimeUnit.SECONDS).readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(false).build(),
    private val resolvers: List<String> = listOf(
        "https://eric.kaspa.stream", "https://john.kaspa.red", "https://noah.kaspa.blue"
    )
) : NetworkFallback {
    override suspend fun fetch(): NetworkReading? = withContext(Dispatchers.IO) {
        withTimeoutOrNull(24_000L) {
            for (resolver in resolvers.shuffled()) {
                try {
                    val record = client.newCall(Request.Builder()
                        .url(resolver + "/v2/kaspa/mainnet/tls/wrpc/borsh").build()).execute().use {
                        if (!it.isSuccessful) null else it.body?.string()?.let { body ->
                            JsonParser.parseString(body).asJsonObject
                        }
                    } ?: continue
                    val url = RpcParser.jsonEndpoint(record.get("url").asString) ?: continue
                    val result = withTimeoutOrNull(10_000L) { readNode(url) }
                    if (result != null) return@withTimeoutOrNull result.copy(source = result.source + ":" + (record.get("uid")?.asString ?: "unknown"))
                } catch (e: CancellationException) { throw e } catch (_: Exception) { }
            }
            null
        }
    }

    internal suspend fun readNode(url: String): NetworkReading? {
        val ready = CompletableDeferred<Unit>()
        val responses = (1..3).associateWith { CompletableDeferred<JsonObject?>() }
        val methods = mapOf(1 to "getServerInfo", 2 to "getBlockDagInfo",
            3 to "estimateNetworkHashesPerSecond")
        val socket = client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) { ready.complete(Unit) }
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    if (text.length > 65_536) throw IOException("Oversized RPC response")
                    val message = JsonParser.parseString(text).asJsonObject
                    val id = message.get("id")?.asInt ?: return
                    val expected = methods[id] ?: return
                    val method = message.get("method")?.asString
                    if (method != null && method != expected) return
                    val result = if (message.get("error")?.isJsonNull == false) null else message.getAsJsonObject("params")
                    responses[id]?.complete(result)
                } catch (_: Exception) {
                    responses.values.forEach { it.complete(null) }
                }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                ready.completeExceptionally(IOException("Node connection unavailable", t))
                responses.values.forEach { it.complete(null) }
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                ready.completeExceptionally(IOException("Node closed"))
                responses.values.forEach { it.complete(null) }
            }
        })
        fun send(id: Int, params: JsonObject = JsonObject()) {
            val request = JsonObject().apply {
                addProperty("id", id); addProperty("method", methods.getValue(id)); add("params", params)
            }
            if (!socket.send(request.toString())) throw IOException("Node request unavailable")
        }
        return try {
            ready.await()
            send(1)
            val info = responses.getValue(1).await() ?: return null
            if (!RpcParser.mainnetSynced(info)) return null
            send(2)
            send(3, JsonObject().apply { addProperty("windowSize", 1000); add("startHash", null) })
            val dag = responses.getValue(2).await()
            val hash = responses.getValue(3).await()
            val network = dag?.let { RpcParser.network(it) }
            val hashrate = hash?.let { RpcParser.hashrate(it) } ?: "N/A"
            if (network == null && hashrate == "N/A") null else
                NetworkReading(network, hashrate, "RPC:" + url.substringBefore("/kaspa"))
        } finally {
            socket.cancel()
        }
    }
}
