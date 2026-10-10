package com.kaspa.livewidget.api

import org.junit.Assert.*
import org.junit.Test

class ExchangeQuoteParserTest {
    private val now = 1_791_601_200_000L
    private fun stats(symbol: String = "KAS-USDT", time: Long = now, rate: String = "-0.0114") =
        """{"code":"200000","data":{"symbol":"$symbol","time":$time,"last":"0.0413","changeRate":"$rate"}}"""

    @Test fun kucoinFractionBecomesPercentWithProviderTime() {
        val quote = ExchangeQuoteParser.parse(stats(time = now - 1000), now)!!
        assertEquals(.0413, quote.price, .000001)
        assertEquals(-1.14, quote.change24h!!, .000001)
        assertEquals(now - 1000, quote.timestamp)
        assertEquals(0.0, ExchangeQuoteParser.parse(stats(rate = "0"), now)!!.change24h!!, 0.0)
    }

    @Test fun wrongPairStaleAndFutureStatisticsAreRejected() {
        assertNull(ExchangeQuoteParser.parse(stats(symbol = "BTC-USDT"), now))
        assertNull(ExchangeQuoteParser.parse(stats(time = now - 31 * 60_000L), now))
        assertNull(ExchangeQuoteParser.parse(stats(time = now + 300_001L), now))
        assertNull(ExchangeQuoteParser.parse(stats(time = 0), now))
        for (rate in listOf("NaN", "Infinity", "bad", "-1.1")) {
            assertNull(ExchangeQuoteParser.parse(stats(rate = rate), now)!!.change24h)
        }
    }

    @Test fun gateAlreadyReportsPercentAndLevelOneRemainsPriceOnly() {
        val gate = ExchangeQuoteParser.parse("""[{"currency_pair":"KAS_USDT","last":"0.0413","change_percentage":"2.5"}]""", now)!!
        assertEquals(2.5, gate.change24h!!, 0.0)
        val priceOnly = ExchangeQuoteParser.parse("""{"code":"200000","data":{"price":"0.0413"}}""", now)!!
        assertNull(priceOnly.change24h)
        assertNull(ExchangeQuoteParser.parse("not JSON", now))
    }
}
