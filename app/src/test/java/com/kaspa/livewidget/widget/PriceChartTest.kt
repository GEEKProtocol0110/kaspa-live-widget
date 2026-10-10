package com.kaspa.livewidget.widget

import com.kaspa.livewidget.data.PricePoint
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[26,34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PriceChartTest {
    @Test fun missingInvalidOrSingleObservationNeverDrawsAPriceLine() {
        val grid=PriceChart.glass(emptyList(),false,280,360)
        val invalid=PriceChart.glass(listOf(PricePoint(1000,Double.NaN),PricePoint(2000,-1.0)),false,280,360)
        val single=PriceChart.glass(listOf(PricePoint(1000,.03)),false,280,360)
        assertTrue(grid.sameAs(invalid))
        assertTrue(grid.sameAs(single))
        val real=PriceChart.glass(listOf(PricePoint(1000,.03),PricePoint(2000,.04)),false,280,360)
        assertFalse(grid.sameAs(real))
        assertEquals(280,real.width);assertEquals(360,real.height)
    }
    @Test fun flatQuotesAndBothThemesRenderWithoutInventingVariations() {
        val points=listOf(PricePoint(1000,.03),PricePoint(2000,.03))
        for(light in listOf(false,true)) {
            val real=PriceChart.glass(points,light,500,600)
            assertFalse(real.sameAs(PriceChart.glass(emptyList(),light,500,600)))
            assertEquals(0,android.graphics.Color.alpha(real.getPixel(0,0)))
        }
    }
}
