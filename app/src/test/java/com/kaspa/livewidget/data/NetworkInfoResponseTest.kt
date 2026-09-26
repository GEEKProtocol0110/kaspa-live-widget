package com.kaspa.livewidget.data

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkInfoResponseTest {
    @Test fun parsesKaspaRestResponseWithDecimalDifficultyAndStringCounters() {
        val json = """{
          "networkName": "kaspa-mainnet",
          "blockCount": "549865202",
          "difficulty": 3870677677777.2,
          "headerCount": "549865202",
          "virtualDaaScore": "549864000"
        }"""

        val info = Gson().fromJson(json, NetworkInfoResponse::class.java)
        assertEquals(549865202L, info.blockCount)
        assertEquals(549864000L, info.virtualDaaScore)
    }
}
