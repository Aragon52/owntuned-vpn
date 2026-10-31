package com.example.data.models

// App info for split tunneling
data class AppInfo(
    val packageName: String,
    val appName: String,
    val isBypassingVpn: Boolean = false
)
