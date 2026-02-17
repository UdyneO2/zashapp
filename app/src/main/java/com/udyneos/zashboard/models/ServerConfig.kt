package com.udyneos.zashboard.models

import android.os.Parcel
import android.os.Parcelable

data class ServerConfig(
    val hostname: String = "127.0.0.1",
    val port: Int = 9090,
    val secret: String = "",
    val secondaryPath: String = "",
    val disableUpgradeCore: Boolean = false,
    val useHttps: Boolean = false
) : Parcelable {
    
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "127.0.0.1",
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte()
    )
    
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(hostname)
        parcel.writeInt(port)
        parcel.writeString(secret)
        parcel.writeString(secondaryPath)
        parcel.writeByte(if (disableUpgradeCore) 1 else 0)
        parcel.writeByte(if (useHttps) 1 else 0)
    }
    
    override fun describeContents(): Int = 0
    
    fun toUrlParams(): String {
        return buildString {
            append("hostname=$hostname")
            append("&port=$port")
            if (secret.isNotEmpty()) {
                append("&secret=$secret")
            }
            if (secondaryPath.isNotEmpty()) {
                append("&secondaryPath=$secondaryPath")
            }
            if (disableUpgradeCore) {
                append("&disableUpgradeCore=true")
            }
        }
    }
    
    fun toFullUrl(): String {
        val protocol = if (useHttps) "https" else "http"
        return "$protocol://$hostname:$port/ui/"
    }
    
    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<ServerConfig> {
            override fun createFromParcel(parcel: Parcel): ServerConfig = ServerConfig(parcel)
            override fun newArray(size: Int): Array<ServerConfig?> = arrayOfNulls(size)
        }
        
        fun fromUrlParams(params: Map<String, String>): ServerConfig {
            return ServerConfig(
                hostname = params["hostname"] ?: "127.0.0.1",
                port = params["port"]?.toIntOrNull() ?: 9090,
                secret = params["secret"] ?: "",
                secondaryPath = params["secondaryPath"] ?: "",
                disableUpgradeCore = params["disableUpgradeCore"] == "true" || params["disableUpgradeCore"] == "1",
                useHttps = params["useHttps"] == "true"
            )
        }
    }
}
