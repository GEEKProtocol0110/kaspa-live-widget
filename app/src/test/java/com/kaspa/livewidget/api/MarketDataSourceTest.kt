package com.kaspa.livewidget.api

import com.google.gson.JsonParser
import com.kaspa.livewidget.data.*
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class MarketDataSourceTest {
    private val now = 1_791_601_200_000L
    @Test fun selectedCurrencyChangeAndProviderTimeArePreserved() {
        val value = MarketParser.quote("""{"kaspa":{"usd":0.0312,"eur":0.029,"eur_24h_change":-2.3,"last_updated_at":1791601200}}""", "EUR", now)!!
        assertEquals(.029, value.price, .000001)
        assertEquals(-2.3, value.change24h!!, .00001)
        assertEquals(now, value.timestamp)
        assertEquals("EUR", value.currency)
        assertNull(MarketParser.quote("""{"kaspa":{"usd":-1,"last_updated_at":1791601200}}""", "USD", now))
        assertNull(MarketParser.quote("""{"kaspa":{"usd":1,"last_updated_at":1791701200}}""", "USD", now))
    }
    @Test fun historyRequiresAnActualDayOfValidTimestampedPrices() {
        val first = now - 24 * 3600_000L
        val valid = """{"prices":[[$now,0.03],[$first,0.02]]}"""
        val points = MarketParser.history(valid, now)!!
        assertEquals(first, points.first().timestamp)
        assertNull(MarketParser.history("""{"prices":[[${now-1000},0.02],[$now,0.03]]}""", now))
        assertNull(MarketParser.history(valid.replace("0.03", "-1"), now))
        assertNull(MarketParser.history(valid, now + 3 * 3600_000L))
    }
    @Test fun exchangeFallbackCannotRelabelUsdAsEur() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(429))
            server.enqueue(MockResponse().setResponseCode(429))
            server.enqueue(MockResponse().setBody("""[{"currency_pair":"KAS_USDT","last":"0.0312","change_percentage":"3.21"}]"""))
            server.start()
            val source = MarketDataSource(coinGeckoBase = server.url("/").toString().removeSuffix("/"),
                exchangeUrls = listOf(server.url("/gate").toString()))
            assertNull(source.quote("EUR", now))
            assertEquals(1, server.requestCount)
            val usd = source.quote("USD", now)!!
            assertEquals("KAS/USDT ≈ USD", usd.source)
            assertEquals(3.21, usd.change24h!!, .00001)
            assertEquals(3, server.requestCount)
        }
    }
    @Test fun outagePreservesAgesAndPriceOnlyFallbackClearsOldChange() {
        val old = MarketData(price=.03, change24h=2.0, timestamp=now-1000, changeTimestamp=now-1000,
            history=listOf(PricePoint(now-24*3600_000L,.02),PricePoint(now,.03)),historyTimestamp=now-1000)
        assertEquals(old, MarketDataMerger.merge(old,"USD",null,null,now))
        val updated = MarketDataMerger.merge(old,"USD",MarketQuote("USD",.04,null,now,"Exchange"),null,now)
        assertNull(updated.change24h)
        assertEquals(0L,updated.changeTimestamp)
        assertEquals(old.historyTimestamp,updated.historyTimestamp)
        assertEquals(0.0,MarketDataMerger.merge(old,"EUR",null,null,now).price,0.0)
        assertEquals(old, MarketDataMerger.merge(old,"USD",MarketQuote("USD",.01,1.0,now-2000,"Old"),null,now))
    }
}
