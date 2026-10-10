package com.kaspa.livewidget.data

/** Retains last good readings without renewing their successful-fetch timestamps. */
internal object NetworkDataMerger {
    fun merge(
        previous: KaspaNetworkData?,
        price: Double?,
        network: NetworkInfoResponse?,
        hashrate: String,
        now: Long
    ): KaspaNetworkData? {
        val priceValid = price != null && price.isFinite() && price > 0.0
        val networkValid = (network?.blockCount ?: 0L) > 0L &&
            (network?.virtualDaaScore ?: 0L) > 0L
        val hashValid = hashrate != "N/A" && hashrate.isNotBlank()
        if (!priceValid && !networkValid && !hashValid) return null

        val old = previous ?: KaspaNetworkData()
        // Materialize legacy timestamps before updating the overall snapshot time.
        fun priorTime(feedTime: Long): Long = feedTime.takeIf { it > 0L } ?: old.timestamp
        return KaspaNetworkData(
            price = if (priceValid) price!! else old.price,
            blockHeight = if (networkValid) network!!.virtualDaaScore!! else old.blockHeight,
            blockCount = if (networkValid) network!!.blockCount!! else old.blockCount,
            bps = if (networkValid) BlockRate.between(previous, network!!.blockCount!!, now) else old.bps,
            hashrate = if (hashValid) hashrate else old.hashrate,
            timestamp = now,
            priceTimestamp = if (priceValid) now else priorTime(old.priceTimestamp),
            networkTimestamp = if (networkValid) now else priorTime(old.networkTimestamp),
            hashrateTimestamp = if (hashValid) now else priorTime(old.hashrateTimestamp)
        )
    }
}
