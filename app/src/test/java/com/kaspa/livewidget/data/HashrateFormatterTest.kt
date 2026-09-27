package com.kaspa.livewidget.data

import org.junit.Assert.assertEquals
import org.junit.Test

class HashrateFormatterTest {
    @Test fun formatsKaspaApiTerahashValuesWithoutMislabelingThemAsMegahashes() {
        assertEquals("354.82 PH/s", HashrateFormatter.formatThPerSecond(354_820.0))
        assertEquals("820.00 TH/s", HashrateFormatter.formatThPerSecond(820.0))
        assertEquals("1.50 EH/s", HashrateFormatter.formatThPerSecond(1_500_000.0))
        assertEquals("N/A", HashrateFormatter.formatThPerSecond(0.0))
    }
}
