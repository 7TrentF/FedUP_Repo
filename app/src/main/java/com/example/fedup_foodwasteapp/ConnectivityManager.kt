package com.example.fedup_foodwasteapp

import android.content.Context
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.ConnectivityManager
import android.util.Log


class ConnectivityManager(
    private val syncService: SyncService,
    context: Context
) {
    private var isRegistered = false
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    private var isConnected = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.d("flow", "Network available, triggering sync")
            isConnected = true
            syncService.startSync()
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            Log.d("flow", "Network lost")
            isConnected = false
        }
    }

    fun startListening() {
        if (!isRegistered) {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
            isRegistered = true
        }
    }

    fun stopListening() {
        if (isRegistered) {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            isRegistered = false
        }
    }

    fun isNetworkAvailable(): Boolean {
        return isConnected
    }
}