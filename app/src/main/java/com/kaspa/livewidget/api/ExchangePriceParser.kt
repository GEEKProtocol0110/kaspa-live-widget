package com.kaspa.livewidget.api

import com.google.gson.JsonParser

/** Parses only the documented KuCoin object and Gate KAS/USDT ticker array. */
internal object ExchangePriceParser {
    fun parse(json: String): Double? = try {
        val root = JsonParser.parseString(json)
        val value = when {
            root.isJsonObject -> {
                val envelope = root.asJsonObject
                if (envelope.get("code")?.asString != "200000") null
                else envelope.getAsJsonObject("data")?.get("price")?.asString
            }
            root.isJsonArray -> root.asJsonArray.firstOrNull {
                it.isJsonObject && it.asJsonObject.get("currency_pair")?.asString == "KAS_USDT"
            }?.asJsonObject?.get("last")?.asString
            else -> null
        }
        value?.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
    } catch (_: Exception) {
        null
    }
}
