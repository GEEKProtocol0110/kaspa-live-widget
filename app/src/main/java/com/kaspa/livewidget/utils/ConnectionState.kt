package com.kaspa.livewidget.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

internal object ConnectionState {
    // A connected interface is not proof that a provider is reachable. Outages
    // still use the worker's partial/unavailable result and retain cached data.
    fun offline(context: Context): Boolean = runCatching {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return@runCatching true
        manager.getNetworkCapabilities(network)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == false
    }.getOrDefault(false)
}
