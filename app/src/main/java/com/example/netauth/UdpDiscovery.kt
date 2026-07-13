package com.example.netauth

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

object UdpDiscovery {

    private fun getBroadcastAddress(context: Context): InetAddress? {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val dhcp = wifiManager.dhcpInfo ?: return null
            val broadcast = (dhcp.ipAddress and dhcp.netmask) or dhcp.netmask.inv()
            val quads = ByteArray(4)
            for (k in 0..3) {
                quads[k] = ((broadcast shr k * 8) and 0xFF).toByte()
            }
            return InetAddress.getByAddress(quads)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    suspend fun discoverServer(context: Context): String? = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.broadcast = true
            socket.soTimeout = 3000 // 3 seconds timeout

            val sendData = "NETAUTH_DISCOVER".toByteArray()
            
            // Try specific subnet broadcast first if available
            val specificBroadcast = getBroadcastAddress(context)
            if (specificBroadcast != null) {
                try {
                    val sendPacket1 = DatagramPacket(sendData, sendData.size, specificBroadcast, 8888)
                    socket.send(sendPacket1)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Fallback to 255.255.255.255
            try {
                val address = InetAddress.getByName("255.255.255.255")
                val sendPacket2 = DatagramPacket(sendData, sendData.size, address, 8888)
                socket.send(sendPacket2)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val receiveData = ByteArray(1024)
            val receivePacket = DatagramPacket(receiveData, receiveData.size)
            socket.receive(receivePacket)

            val message = String(receivePacket.data, 0, receivePacket.length)
            if (message.startsWith("NETAUTH_SERVER:")) {
                return@withContext message.substringAfter("NETAUTH_SERVER:").trim()
            }
        } catch (e: SocketTimeoutException) {
            // Timeout reached
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            socket?.close()
        }
        null
    }
}

