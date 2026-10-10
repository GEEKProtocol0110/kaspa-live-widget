package com.kaspa.livewidget.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kaspa.livewidget.api.KaspaApiService
import com.kaspa.livewidget.api.MarketDataSource
import com.kaspa.livewidget.data.MarketDataMerger
import com.kaspa.livewidget.data.NetworkDataMerger
import com.kaspa.livewidget.utils.*
import com.kaspa.livewidget.widget.WidgetRenderer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WidgetUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    companion object { private val refreshMutex = Mutex() }

    override suspend fun doWork(): Result = refreshMutex.withLock {
        val context = applicationContext
        val settings = WidgetSettings(context).get()
        val status = RefreshStatusCache(context)
        status.set("running", settings.currency)
        WidgetRenderer.updateAll(context)
        try {
            coroutineScope {
                val now = System.currentTimeMillis()
                val marketCache = MarketCache(context)
                val networkCache = DataCache(context)
                val oldMarket = marketCache.get(settings.currency)
                val source = MarketDataSource()
                val quote = async { source.quote(settings.currency, now) }
                val history = async {
                    if (settings.showChart && (oldMarket.history.isEmpty() || now - oldMarket.historyTimestamp >= 3_600_000L))
                        source.history(settings.currency, now) else null
                }
                val feeds = async { KaspaApiService().fetchFeeds() }
                val newQuote = quote.await()
                val newHistory = history.await()
                val network = feeds.await()
                val market = MarketDataMerger.merge(oldMarket, settings.currency, newQuote, newHistory, now)
                marketCache.save(market)
                val merged = NetworkDataMerger.merge(networkCache.getCachedData(),
                    if (settings.currency == "USD") newQuote?.price else null,
                    network.network, network.hashrate, System.currentTimeMillis(),
                    network.networkSource, network.hashrateSource)
                if (merged != null) networkCache.saveData(merged)
                val successes = listOf(newQuote != null, network.network != null, network.hashrate != "N/A")
                status.set(if (successes.all { it }) "complete" else if (successes.any { it }) "partial" else "unavailable", settings.currency)
            }
            WidgetRenderer.updateAll(context)
            // Completed outages release manual KEEP work so the next tap can retry.
            Result.success()
        } catch (e: CancellationException) {
            status.set("interrupted", settings.currency)
            WidgetRenderer.updateAll(context)
            throw e
        } catch (e: Exception) {
            status.set("unavailable", settings.currency)
            WidgetRenderer.updateAll(context)
            Result.failure()
        }
    }
}
