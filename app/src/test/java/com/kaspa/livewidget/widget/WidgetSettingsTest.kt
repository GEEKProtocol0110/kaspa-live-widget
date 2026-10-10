package com.kaspa.livewidget.widget

import android.content.Context
import android.widget.*
import com.kaspa.livewidget.R
import com.kaspa.livewidget.SettingsActivity
import com.kaspa.livewidget.data.*
import com.kaspa.livewidget.utils.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[26,34])
class WidgetSettingsTest {
    private val context get() = RuntimeEnvironment.getApplication()
    @Test fun settingsNormalizeAndCurrencyCachesStayIndependent() {
        val settings = WidgetSettings(context)
        settings.save(WidgetPreferences("XYZ",2,"missing",true))
        assertEquals(WidgetPreferences(showChart=true),settings.get())
        settings.save(WidgetPreferences("EUR",60,"light",true))
        assertEquals("EUR",settings.get().currency)
        DataCache(context).saveData(KaspaNetworkData(price=.04,priceTimestamp=1000,timestamp=2000))
        val cache = MarketCache(context)
        assertEquals(.04,cache.get("USD").price,0.0)
        assertEquals(1000L,cache.get("USD").timestamp)
        assertEquals(0.0,cache.get("EUR").price,0.0)
        cache.save(MarketData(currency="EUR",price=.03,timestamp=1500))
        assertEquals(.03,cache.get("EUR").price,0.0)
        assertEquals(.04,cache.get("USD").price,0.0)
        context.getSharedPreferences("kaspa_market_cache",Context.MODE_PRIVATE).edit().putString("GBP","not json").commit()
        assertEquals(0.0,cache.get("GBP").price,0.0)
    }
    @Test fun progressExpiryAndMissingChangeCannotLookLikeSuccess() {
        assertTrue(RefreshStatus("running",1000).inProgress(1100))
        assertFalse(RefreshStatus("running",1000).inProgress(122000))
        assertEquals("Tap to retry",RefreshStatus("running",1000).label("USD",122000))
        assertEquals("Tap to load EUR",RefreshStatus("complete",1000).label("EUR",1100))
        assertTrue(RefreshStatus("unavailable",1000).label("USD").contains("unavailable"))
        assertEquals("24h change unavailable",MarketDisplay.change(MarketData()))
        assertTrue(MarketDisplay.change(MarketData(change24h=2.0,changeTimestamp=1000),1900000).contains("stale"))
    }
    @Test fun appLaunchesWithSavedChoicesAndWidgetInstallControls() {
        WidgetSettings(context).save(WidgetPreferences("GBP",30,"light",true))
        Robolectric.buildActivity(SettingsActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            assertEquals("GBP",activity.findViewById<Spinner>(R.id.currency_choice).selectedItem)
            assertEquals("30 minutes",activity.findViewById<Spinner>(R.id.interval_choice).selectedItem)
            assertEquals("Frosted light",activity.findViewById<Spinner>(R.id.appearance_choice).selectedItem)
            assertTrue(activity.findViewById<Switch>(R.id.chart_choice).isChecked)
            assertNotNull(activity.findViewById<Button>(R.id.add_compact))
            assertNotNull(activity.findViewById<Button>(R.id.add_expanded))
        }
    }
}
