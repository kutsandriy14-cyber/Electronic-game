package com.example.netauth

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException

object UdpDiscovery {

    private fun getBroadcastAddresses(context: Context): List<InetAddress> {
        val addresses = mutableListOf<InetAddress>()
        
        // 1. Use Java NetworkInterface (highly robust, handles endianness and permissions perfectly)
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val netInterface = interfaces.nextElement()
                if (netInterface.isLoopback || !netInterface.isUp) continue
                for (interfaceAddress in netInterface.interfaceAddresses) {
                    val broadcast = interfaceAddress.broadcast
                    if (broadcast != null) {
                        addresses.add(broadcast)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Fallback to DhcpInfo if NetworkInterface was empty
        if (addresses.isEmpty()) {
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val dhcp = wifiManager.dhcpInfo
                if (dhcp != null && dhcp.ipAddress != 0) {
                    // Reverse bytes of dhcp.ipAddress if it's stored in little-endian format
                    val ip = dhcp.ipAddress
                    val mask = dhcp.netmask
                    val ipReversed = java.lang.Integer.reverseBytes(ip)
                    val maskReversed = java.lang.Integer.reverseBytes(mask)
                    
                    val broadcastVal = (ipReversed and maskReversed) or maskReversed.inv()
                    val quads = ByteArray(4)
                    for (k in 0..3) {
                        quads[k] = ((broadcastVal shr ((3 - k) * 8)) and 0xFF).toByte()
                    }
                    val addr = InetAddress.getByAddress(quads)
                    addresses.add(addr)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Absolute fallback to 255.255.255.255
        if (addresses.isEmpty()) {
            try {
                addresses.add(InetAddress.getByName("255.255.255.255"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return addresses
    }

    suspend fun discoverServer(context: Context): String? = withContext(Dispatchers.IO) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val multicastLock = try {
            wifiManager.createMulticastLock("NetAuthUdpDiscoveryLock").apply {
                setReferenceCounted(false)
            }
        } catch (e: Exception) {
            null
        }

        try {
            multicastLock?.acquire()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.broadcast = true
            socket.soTimeout = 2500 // 2.5 seconds timeout is optimal

            val sendData = "NETAUTH_DISCOVER".toByteArray()
            val broadcastAddresses = getBroadcastAddresses(context)
            
            for (broadcastAddr in broadcastAddresses) {
                try {
                    val sendPacket = DatagramPacket(sendData, sendData.size, broadcastAddr, 8888)
                    socket.send(sendPacket)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Also explicitly send to global broadcast just in case
            try {
                val globalBroadcast = InetAddress.getByName("255.255.255.255")
                if (!broadcastAddresses.contains(globalBroadcast)) {
                    val sendPacket = DatagramPacket(sendData, sendData.size, globalBroadcast, 8888)
                    socket.send(sendPacket)
                }
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
            // Timeout reached, normal if server is not online
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            socket?.close()
            try {
                if (multicastLock != null && multicastLock.isHeld) {
                    multicastLock.release()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        null
    }
}

