package com.hindrax.ss.core.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkUtils {
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()
            // Prefer wlan0 (WiFi)
            val wlan = interfaces.find { it.name.contains("wlan") } ?: interfaces.find { !it.isLoopback && it.isUp }
            
            wlan?.inetAddresses?.asSequence()?.forEach { address ->
                if (!address.isLoopbackAddress && address is Inet4Address) {
                    return address.hostAddress
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
