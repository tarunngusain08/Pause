package com.pause.app.service.webfilter

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

@AndroidEntryPoint
class PauseVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
            .setSession("Pause Web Filter")
            .addAddress("10.0.0.1", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("10.0.0.1")
            .setMtu(1500)
            .addDisallowedApplication(packageName)
        vpnInterface = config.establish()
        if (vpnInterface != null) {
            startForeground(NOTIFICATION_ID, createNotification())
            scope.launch { runDnsLoop() }
        }
    }

    private fun stopVpn() {
        vpnInterface?.close()
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun runDnsLoop() {
        val vpnFd = vpnInterface ?: return
        val entryPoint = EntryPointAccessors.fromApplication(this, VpnEntryPoint::class.java)
        val blocklistMatcher = entryPoint.getBlocklistMatcher()
        val whitelistMatcher = entryPoint.getWhitelistMatcher()
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
                            val cfg = if (++loopCount % 100 == 0) {
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
                                whitelistMatcher.isWhitelisted(domain) ->
                                    resolveUpstream(upstream, dnsPayload) ?: continue
                                blocklistMatcher.isBlocked(domain) ->
                                    DNSPacketParser.buildNXDomainResponse(query)
                                else ->
                                    resolveUpstream(upstream, dnsPayload) ?: continue
                            }
                            if (dnsResponse.isNotEmpty()) {
                                output.write(DNSPacketParser.wrapResponse(dnsResponse, ipUdpInfo))
                            }
                        } catch (_: Exception) { }
                    }
                }
            }
        } catch (_: Exception) { }
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
            .setContentTitle("Pause Web Filter")
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
    }
}
