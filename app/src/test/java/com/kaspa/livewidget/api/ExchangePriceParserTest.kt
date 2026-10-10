package com.kaspa.livewidget.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExchangePriceParserTest {
    @Test fun acceptsDocumentedKuCoinAndGateShapes() {
        assertEquals(0.0312, ExchangePriceParser.parse("""{"code":"200000","data":{"price":"0.0312"}}""")!!, 0.0)
        assertEquals(0.0313, ExchangePriceParser.parse("""[{"currency_pair":"BTC_USDT","last":"60000"},{"currency_pair":"KAS_USDT","last":"0.0313"}]""")!!, 0.0)
    }

    @Test fun rejectsErrorsOtherPairsAndInvalidPrices() {
        for (json in listOf(
            """{"code":"429000","data":{"price":"0.03"}}""",
            """[{"currency_pair":"BTC_USDT","last":"60000"}]""",
            """[{"last":"0.03"}]""", "[]", "null", "not json"
        )) assertNull(ExchangePriceParser.parse(json))
        for (price in listOf("NaN", "Infinity", "-1", "0", "bad")) {
            assertNull(ExchangePriceParser.parse("""[{"currency_pair":"KAS_USDT","last":"$price"}]"""))
        }
    }
}
