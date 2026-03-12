package com.pause.app.service.webfilter

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.pause.app.MainActivity
import com.pause.app.R
import com.pause.app.di.VpnEntryPoint
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket

@AndroidEntryPoint
class PauseVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val heartbeatPrefs: SharedPreferences by lazy {
        getSharedPreferences(HEARTBEAT_PREFS, Context.MODE_PRIVATE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startVpn()
            ACTION_STOP -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (vpnInterface != null) return
        val config = Builder()
            .setSession("Focus Web Filter")
            .addAddress("10.0.0.1", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("10.0.0.1")
            .setMtu(1500)
            .addDisallowedApplication(packageName)
        vpnInterface = config.establish()
        if (vpnInterface != null) {
            startForeground(NOTIFICATION_ID, createNotification())
            scope.launch { runBlockPageServer() }
            scope.launch { runDnsLoop() }
        }
    }

    private fun stopVpn() {
        vpnInterface?.close()
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun runBlockPageServer() {
        try {
            ServerSocket(BLOCK_PAGE_PORT).use { serverSocket ->
                serverSocket.reuseAddress = true
                while (vpnInterface != null) {
                    try {
                        val client = serverSocket.accept()
                        scope.launch(Dispatchers.IO) {
                            try {
                                client.use { socket ->
                                    val reader = socket.getInputStream().bufferedReader()
                                    val requestLine = reader.readLine() ?: return@launch
                                    var host = "blocked"
                                    var line = reader.readLine()
                                    while (!line.isNullOrBlank()) {
                                        if (line.startsWith("Host:", ignoreCase = true)) {
                                            host = line.substringAfter(":").trim().substringBefore(":")
                                        }
                                        line = reader.readLine()
                                    }
                                    val domain = host
                                    val safeDomain = domain
                                        .replace("&", "&amp;")
                                        .replace("<", "&lt;")
                                        .replace(">", "&gt;")
                                        .replace("\"", "&quot;")
                                    val html = buildBlockPage(safeDomain)
                                    val response = buildString {
                                        append("HTTP/1.1 200 OK\r\n")
                                        append("Content-Type: text/html; charset=utf-8\r\n")
                                        append("Content-Length: ${html.toByteArray().size}\r\n")
                                        append("Connection: close\r\n")
                                        append("\r\n")
                                        append(html)
                                    }
                                    socket.getOutputStream().write(response.toByteArray())
                                }
                            } catch (_: Exception) { }
                        }
                    } catch (_: Exception) { }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Block page server stopped", e)
        }
    }

    private fun buildBlockPage(domain: String): String = """
        <!DOCTYPE html>
        <html>
        <head><meta charset="utf-8"><title>Blocked by Focus</title>
        <style>body{font-family:sans-serif;display:flex;align-items:center;justify-content:center;
        height:100vh;margin:0;background:#1a1a2e;}
        .card{background:#fff;border-radius:12px;padding:32px;text-align:center;max-width:400px;}
        h1{color:#e74c3c}p{color:#555}</style></head>
        <body><div class="card">
        <h1>Blocked</h1>
        <p><strong>$domain</strong> is blocked by Focus Web Filter.</p>
        <p>This site has been blocked to help you stay focused.</p>
        </div></body>
        </html>
    """.trimIndent()

    private suspend fun runDnsLoop() {
        val vpnFd = vpnInterface ?: return
        val entryPoint = EntryPointAccessors.fromApplication(this, VpnEntryPoint::class.java)
        val blocklistMatcher = entryPoint.getBlocklistMatcher()
        val configRepo = entryPoint.getWebFilterConfigRepository()

        var config = configRepo.getConfig()
        if (config == null || !config.vpnEnabled) {
            stopVpn()
            return
        }

        val buffer = ByteArray(2048)
        var loopCount = 0
        try {
            FileInputStream(vpnFd.fileDescriptor).use { input ->
                FileOutputStream(vpnFd.fileDescriptor).use { output ->
                    while (vpnInterface != null) {
                        try {
                            if (++loopCount % 60 == 0) {
                                heartbeatPrefs.edit()
                                    .putLong(KEY_LAST_HEARTBEAT, System.currentTimeMillis())
                                    .apply()
                            }
                            val cfg = if (loopCount % 100 == 0) {
                                val newConfig = configRepo.getConfig()
                                if (newConfig == null || !newConfig.vpnEnabled) {
                                    stopVpn()
                                    return
                                }
                                config = newConfig
                                newConfig
                            } else {
                                config
                            } ?: return
                            val read = input.read(buffer)
                            if (read <= 0) continue
                            val packet = buffer.copyOf(read)
                            val ipUdpInfo = DNSPacketParser.extractDnsInfo(packet) ?: continue
                            val dnsPayload = packet.copyOfRange(
                                ipUdpInfo.dnsOffset,
                                ipUdpInfo.dnsOffset + ipUdpInfo.dnsLength
                            )
                            val query = DNSPacketParser.parseQuery(dnsPayload) ?: continue
                            val domain = query.question
                            val upstream = cfg.upstreamDns.ifBlank { "8.8.8.8" }
                            val dnsResponse: ByteArray = when {
                                blocklistMatcher.isBlocked(domain) ->
                                    DNSPacketParser.buildRedirectResponse(query, BLOCK_PAGE_HOST)
                                else ->
                                    resolveUpstream(upstream, dnsPayload) ?: continue
                            }
                            if (dnsResponse.isNotEmpty()) {
                                output.write(DNSPacketParser.wrapResponse(dnsResponse, ipUdpInfo))
                            }
                        } catch (e: java.io.IOException) {
                            // fd was closed or revoked; stop the loop
                            Log.w(TAG, "VPN fd I/O error, stopping loop", e)
                            break
                        } catch (e: Exception) {
                            Log.w(TAG, "DNS packet processing error (ignored)", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "VPN loop fatal error", e)
        }
    }

    private fun resolveUpstream(upstream: String, packet: ByteArray): ByteArray? {
        return try {
            DatagramSocket().use { socket ->
                socket.soTimeout = 3000
                val addr = InetAddress.getByName(upstream)
                val sendPacket = DatagramPacket(packet, packet.size, addr, 53)
                socket.send(sendPacket)
                val recvBuf = ByteArray(512)
                val recvPacket = DatagramPacket(recvBuf, recvBuf.size)
                socket.receive(recvPacket)
                recvBuf.copyOf(recvPacket.length)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.web_filter_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Web Filter")
            .setContentText("Web filtering active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    companion object {
        const val ACTION_START = "com.pause.app.VPN_START"
        const val ACTION_STOP = "com.pause.app.VPN_STOP"
        private const val NOTIFICATION_ID = 2000
        private const val CHANNEL_ID = "web_filter"
        private const val TAG = "PauseVpnService"
        const val HEARTBEAT_PREFS = "vpn_heartbeat"
        const val KEY_LAST_HEARTBEAT = "vpn_last_heartbeat"
        const val BLOCK_PAGE_PORT = 80
        private const val BLOCK_PAGE_HOST = "127.0.0.1"
    }
}
