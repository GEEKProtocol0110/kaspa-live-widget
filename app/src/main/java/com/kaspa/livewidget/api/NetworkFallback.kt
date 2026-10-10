package com.kaspa.livewidget.api

import com.kaspa.livewidget.data.NetworkInfoResponse

data class NetworkReading(
    val network: NetworkInfoResponse?,
    val hashrate: String,
    val source: String
)

fun interface NetworkFallback {
    suspend fun fetch(): NetworkReading?
}

data class NetworkFeeds(
    val network: NetworkInfoResponse?,
    val hashrate: String,
    val networkSource: String,
    val hashrateSource: String
)

internal object NetworkFeedResolver {
    suspend fun resolve(primary: NetworkInfoResponse?, hash: String,
                        fallback: NetworkFallback): NetworkFeeds {
        val network = primary?.takeIf { (it.blockCount ?: 0) > 0 && (it.virtualDaaScore ?: 0) > 0 }
        val hashValid = hash != "N/A" && hash.isNotBlank()
        val backup = if (network == null || !hashValid) fallback.fetch() else null
        return NetworkFeeds(network ?: backup?.network, if (hashValid) hash else backup?.hashrate ?: "N/A",
            if (network != null) "Kaspa REST" else backup?.source ?: "",
            if (hashValid) "Kaspa REST" else backup?.source ?: "")
    }
}
