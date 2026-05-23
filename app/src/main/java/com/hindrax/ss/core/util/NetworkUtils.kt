package com.hindrax.ss.core.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.InetAddress
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

    fun getLocalSubnetHosts(): List<String> {
        val localIp = getLocalIpAddress() ?: return emptyList()
        val interfaceAddress = runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.interfaceAddresses.asSequence() }
                .firstOrNull { it.address is Inet4Address && it.address.hostAddress == localIp }
        }.getOrNull()

        val prefix = interfaceAddress?.networkPrefixLength?.toInt()
            ?.takeIf { it in 24..30 }

        return if (prefix != null) {
            hostsFromCidr(localIp, prefix)
        } else {
            val subnet = localIp.substringBeforeLast(".", missingDelimiterValue = "")
            if (subnet.isBlank() || localIp == "0.0.0.0") emptyList() else (1..254).map { "$subnet.$it" }
        }.filter { it != localIp }
    }

    private fun hostsFromCidr(ip: String, prefix: Int): List<String> {
        val ipInt = InetAddress.getByName(ip).address.fold(0) { acc, byte ->
            (acc shl 8) or (byte.toInt() and 0xff)
        }
        val mask = -1 shl (32 - prefix)
        val network = ipInt and mask
        val broadcast = network or mask.inv()
        return ((network + 1) until broadcast).map { value ->
            listOf(
                (value ushr 24) and 0xff,
                (value ushr 16) and 0xff,
                (value ushr 8) and 0xff,
                value and 0xff
            ).joinToString(".")
        }
    }
}
