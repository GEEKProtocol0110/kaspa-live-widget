package com.kaspa.livewidget.api

import com.google.gson.JsonObject
import com.kaspa.livewidget.data.HashrateFormatter
import com.kaspa.livewidget.data.NetworkInfoResponse
import java.net.URI

internal object RpcParser {
    fun mainnetSynced(info: JsonObject): Boolean = try {
        info.get("networkId")?.asString in listOf("mainnet", "kaspa-mainnet") &&
            info.get("isSynced")?.asBoolean == true && info.get("rpcApiVersion")?.asInt == 1
    } catch (_: Exception) { false }

    fun network(dag: JsonObject): NetworkInfoResponse? = try {
        if (dag.get("network")?.asString !in listOf("mainnet", "kaspa-mainnet")) null
        else {
            val count = dag.get("blockCount").asString.toLong()
            val daa = dag.get("virtualDaaScore").asString.toLong()
            if (count <= 0 || daa <= 0) null else NetworkInfoResponse(count, daa)
        }
    } catch (_: Exception) { null }

    fun hashrate(result: JsonObject): String = try {
        val hashes = result.get("networkHashesPerSecond").asDouble
        // RPC returns H/s; the REST formatter's input is TH/s.
        HashrateFormatter.formatThPerSecond(hashes / 1_000_000_000_000.0)
    } catch (_: Exception) { "N/A" }

    fun jsonEndpoint(endpoint: String): String? = try {
        val uri = URI(endpoint)
        val host = uri.host?.lowercase() ?: ""
        val allowed = listOf(".kaspa.stream", ".kaspa.red", ".kaspa.green", ".kaspa.blue", ".kaspa.ws")
        if (uri.scheme != "wss" || uri.userInfo != null || uri.port !in listOf(-1, 443) ||
            allowed.none { host.endsWith(it) } || uri.query != null || uri.fragment != null) null
        else when (uri.path) {
            "/kaspa/mainnet/wrpc/json" -> endpoint
            "/kaspa/mainnet/wrpc/borsh" -> endpoint.removeSuffix("/borsh") + "/json"
            else -> null
        }
    } catch (_: Exception) { null }
}
