package com.udyneos.zashboard.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.UnknownHostException

object ClientUtils {
    
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                   capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                   capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }
    
    fun getWifiIpAddress(context: Context): String? {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Untuk Android 13+ gunakan cara yang berbeda
                val dhcpInfo = wifiManager.dhcpInfo
                dhcpInfo?.serverAddress?.let {
                    intToIp(it)
                }
            } else {
                // Untuk Android 12 ke bawah (deprecated tapi masih berfungsi)
                @Suppress("DEPRECATION")
                val ipAddress = wifiManager.connectionInfo?.ipAddress ?: return null
                @Suppress("DEPRECATION")
                intToIp(ipAddress)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun intToIp(ipInt: Int): String {
        return String.format(
            "%d.%d.%d.%d",
            ipInt and 0xFF,
            (ipInt shr 8) and 0xFF,
            (ipInt shr 16) and 0xFF,
            (ipInt shr 24) and 0xFF
        )
    }
    
    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addresses = intf.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                        val hostAddress = addr.hostAddress ?: continue
                        if (hostAddress.indexOf(':') < 0) {
                            return hostAddress
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }
    
    fun isValidHostname(hostname: String): Boolean {
        return try {
            InetAddress.getByName(hostname)
            true
        } catch (e: UnknownHostException) {
            false
        }
    }
    
    fun parseUrlParams(url: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        
        if (!url.contains("#/setup?")) {
            return params
        }
        
        val queryPart = url.substringAfter("#/setup?")
        val pairs = queryPart.split("&")
        
        for (pair in pairs) {
            val keyValue = pair.split("=")
            if (keyValue.size == 2) {
                params[keyValue[0]] = keyValue[1]
            }
        }
        
        return params
    }
    
    fun buildDashboardUrl(baseUrl: String, config: ServerConfig): String {
        val params = mutableMapOf(
            "hostname" to config.hostname,
            "port" to config.port.toString()
        )
        if (config.secret.isNotEmpty()) {
            params["secret"] = config.secret
        }
        if (config.disableUpgradeCore) {
            params["disableUpgradeCore"] = "true"
        }
        
        val paramString = params.map { "${it.key}=${it.value}" }.joinToString("&")
        return "$baseUrl#/setup?$paramString"
    }
    
    data class ServerConfig(
        val hostname: String,
        val port: Int,
        val secret: String,
        val disableUpgradeCore: Boolean
    )
}
