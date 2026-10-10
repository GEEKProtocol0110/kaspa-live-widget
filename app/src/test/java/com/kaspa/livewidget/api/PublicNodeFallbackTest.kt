package com.kaspa.livewidget.api

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.kaspa.livewidget.data.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.*
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

class PublicNodeFallbackTest {
    private fun json(s: String) = JsonParser.parseString(s).asJsonObject
    @Test fun endpointAndNetworkChecksRejectUntrustedOrUnsyncedNodes() {
        assertEquals("wss://vivi.kaspa.blue/kaspa/mainnet/wrpc/json", RpcParser.jsonEndpoint("wss://vivi.kaspa.blue/kaspa/mainnet/wrpc/borsh"))
        for (url in listOf("ws://vivi.kaspa.blue/kaspa/mainnet/wrpc/borsh", "wss://evil.example/kaspa/mainnet/wrpc/borsh", "wss://x.kaspa.blue/other", "wss://x.kaspa.blue/kaspa/testnet/wrpc/json", "wss://x.kaspa.blue/kaspa/mainnet/wrpc/json?redirect=1")) assertNull(RpcParser.jsonEndpoint(url))
        assertTrue(RpcParser.mainnetSynced(json("""{"networkId":"mainnet","isSynced":true,"rpcApiVersion":1}""")))
        assertFalse(RpcParser.mainnetSynced(json("""{"networkId":"testnet-10","isSynced":true,"rpcApiVersion":1}""")))
        assertFalse(RpcParser.mainnetSynced(json("""{"networkId":"mainnet","isSynced":false,"rpcApiVersion":1}""")))
        assertNull(RpcParser.network(json("""{"network":"testnet-10","blockCount":10,"virtualDaaScore":20}""")))
        assertEquals("374.58 PH/s", RpcParser.hashrate(json("""{"networkHashesPerSecond":374579773359291173}""")))
    }
    @Test fun missingFeedsUseBackupWithoutOverwritingHealthyPrimary() = runBlocking {
        val primary = NetworkInfoResponse(100,200)
        var calls = 0
        val fallback = NetworkFallback { calls++; NetworkReading(NetworkInfoResponse(999,888), "374.58 PH/s", "node") }
        val partial = NetworkFeedResolver.resolve(primary,"N/A",fallback)
        assertEquals(primary,partial.network)
        assertEquals("node",partial.hashrateSource)
        assertEquals("Kaspa REST",partial.networkSource)
        NetworkFeedResolver.resolve(primary,"1 PH/s",fallback)
        assertEquals(1,calls)
        val missing = NetworkFeedResolver.resolve(null,"2 PH/s",fallback)
        assertEquals(888L,missing.network!!.virtualDaaScore)
        assertEquals("2 PH/s",missing.hashrate)
        assertNull(NetworkFeedResolver.resolve(null,"N/A",NetworkFallback { null }).network)
    }
    @Test fun sourceSwitchResetsObservedBlockRate() {
        val old = KaspaNetworkData(blockCount=100, blockHeight=200, networkTimestamp=1000, timestamp=1000, networkSource="rest")
        val next = NetworkDataMerger.merge(old,null,NetworkInfoResponse(1000000,300),"N/A",2000,"node")!!
        assertEquals(0.0,next.bps,0.0)
    }
    @Test fun realWebsocketShapeValidatesMainnetBeforeSendingOnlyReadCalls() = runBlocking {
        for (network in listOf("mainnet","testnet-10")) {
            MockWebServer().use { server ->
                val methods = CopyOnWriteArrayList<String>()
                server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                    override fun onMessage(socket: WebSocket, text: String) {
                        val request = json(text)
                        val method = request.get("method").asString
                        methods.add(method)
                        val result = when(method) {
                            "getServerInfo" -> """{"networkId":"$network","isSynced":true,"rpcApiVersion":1}"""
                            "getBlockDagInfo" -> """{"network":"mainnet","blockCount":1210217,"virtualDaaScore":561810469}"""
                            "estimateNetworkHashesPerSecond" -> {
                                assertEquals(1000, request.getAsJsonObject("params").get("windowSize").asInt)
                                """{"networkHashesPerSecond":374579773359291173}"""
                            }
                            else -> error("Unexpected RPC method")
                        }
                        socket.send("""{"id":${request.get("id")},"method":"$method","params":$result,"error":null}""")
                    }
                }))
                server.start()
                val result = withTimeout(5000) { PublicNodeFallback().readNode(server.url("/rpc").toString()) }
                if (network == "mainnet") {
                    assertEquals(561810469L,result!!.network!!.virtualDaaScore)
                    assertEquals("374.58 PH/s",result.hashrate)
                    assertEquals(setOf("getServerInfo","getBlockDagInfo","estimateNetworkHashesPerSecond"),methods.toSet())
                } else {
                    assertNull(result)
                    assertEquals(listOf("getServerInfo"),methods.toList())
                }
            }
        }
    }
}
