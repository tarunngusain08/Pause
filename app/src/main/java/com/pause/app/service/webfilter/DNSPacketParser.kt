package com.pause.app.service.webfilter

import java.nio.ByteBuffer

/**
 * Parses DNS packets and extracts the queried domain name.
 * Builds NXDOMAIN and redirect responses.
 * Handles IPv4+UDP encapsulation for TUN interface packets.
 */
object DNSPacketParser {

    data class IpUdpInfo(
        val srcIp: ByteArray,
        val dstIp: ByteArray,
        val srcPort: Int,
        val dnsOffset: Int,
        val dnsLength: Int
    ) {
        override fun equals(other: Any?) = other is IpUdpInfo &&
            srcIp.contentEquals(other.srcIp) &&
            dstIp.contentEquals(other.dstIp) &&
            srcPort == other.srcPort &&
            dnsOffset == other.dnsOffset &&
            dnsLength == other.dnsLength
        override fun hashCode() = 31 * (31 * srcIp.contentHashCode() + dstIp.contentHashCode()) +
            srcPort + dnsOffset + dnsLength
    }

    /**
     * Extracts IP/UDP metadata and DNS payload offset from an IPv4 packet.
     * Returns null if not a valid UDP packet to port 53.
     */
    fun extractDnsInfo(ipPacket: ByteArray): IpUdpInfo? {
        if (ipPacket.size < 28) return null
        if ((ipPacket[0].toInt() and 0xFF) ushr 4 != 4) return null // IPv4
        val ihl = (ipPacket[0].toInt() and 0x0F) * 4
        if (ihl < 20 || ipPacket.size < ihl + 8) return null
        if ((ipPacket[9].toInt() and 0xFF) != 17) return null // UDP protocol
        val srcPort = ((ipPacket[ihl].toInt() and 0xFF) shl 8) or (ipPacket[ihl + 1].toInt() and 0xFF)
        val dstPort = ((ipPacket[ihl + 2].toInt() and 0xFF) shl 8) or (ipPacket[ihl + 3].toInt() and 0xFF)
        if (dstPort != 53) return null
        val srcIp = ipPacket.copyOfRange(12, 16)
        val dstIp = ipPacket.copyOfRange(16, 20)
        val dnsOffset = ihl + 8
        val dnsLength = (ipPacket.size - dnsOffset).coerceAtLeast(0)
        if (dnsLength < 12) return null
        return IpUdpInfo(srcIp, dstIp, srcPort, dnsOffset, dnsLength)
    }

    /**
     * Wraps a bare DNS response in IPv4+UDP headers, swapping src/dst for reply.
     */
    fun wrapResponse(dnsResponse: ByteArray, info: IpUdpInfo): ByteArray {
        val udpLen = 8 + dnsResponse.size
        val totalLen = 20 + udpLen
        val buf = ByteBuffer.allocate(totalLen)
        buf.put(0x45.toByte()) // version=4, IHL=5
        buf.put(0x00.toByte()) // TOS
        buf.putShort((totalLen and 0xFFFF).toShort())
        buf.putShort(0) // id
        buf.putShort(0x4000.toShort()) // flags, frag offset
        buf.put(64.toByte()) // TTL
        buf.put(17.toByte()) // protocol UDP
        buf.putShort(0) // checksum (0 acceptable for TUN)
        buf.put(info.dstIp) // src IP = original dst
        buf.put(info.srcIp) // dst IP = original src
        buf.putShort(53) // src port = 53
        buf.putShort((info.srcPort and 0xFFFF).toShort()) // dst port = original src
        buf.putShort((udpLen and 0xFFFF).toShort())
        buf.putShort(0) // UDP checksum
        buf.put(dnsResponse)
        buf.flip()
        return ByteArray(buf.remaining()).also { buf.get(it) }
    }

    data class DNSQuery(
        val transactionId: Short,
        val question: String,
        val rawPacket: ByteArray
    ) {
        override fun equals(other: Any?) = other is DNSQuery &&
            transactionId == other.transactionId &&
            question == other.question &&
            rawPacket.contentEquals(other.rawPacket)
        override fun hashCode() = 31 * (31 * transactionId + question.hashCode()) + rawPacket.contentHashCode()
    }

    fun parseQuery(packet: ByteArray): DNSQuery? {
        if (packet.size < 12) return null
        val buf = ByteBuffer.wrap(packet)
        val transactionId = buf.short
        val flags = buf.short
        val qdCount = buf.short.toInt() and 0xFFFF
        if (qdCount == 0) return null
        val question = parseQuestion(packet, 12) ?: return null
        return DNSQuery(transactionId, question, packet)
    }

    private fun parseQuestion(packet: ByteArray, offset: Int): String? {
        val labels = mutableListOf<String>()
        var pos = offset
        while (pos < packet.size) {
            val len = packet[pos].toInt() and 0xFF
            if (len == 0) break
            if (len > 63) return null
            pos++
            if (pos + len > packet.size) return null
            labels.add(String(packet, pos, len, Charsets.US_ASCII))
            pos += len
        }
        return labels.joinToString(".")
    }

    fun buildNXDomainResponse(query: DNSQuery): ByteArray {
        if (query.rawPacket.size <= 12) return ByteArray(0)
        val buf = ByteBuffer.allocate(512)
        buf.putShort(query.transactionId)
        buf.putShort(0x8183.toShort()) // Response, Authoritative, NXDOMAIN
        buf.putShort(1)       // 1 question
        buf.putShort(0)      // 0 answers
        buf.putShort(0)
        buf.putShort(0)
        buf.put(query.rawPacket, 12, query.rawPacket.size - 12)
        buf.flip()
        return ByteArray(buf.remaining()).also { buf.get(it) }
    }

    fun buildRedirectResponse(query: DNSQuery, ip: String): ByteArray {
        val parts = ip.split(".").map { it.toInt().toByte() }
        if (parts.size != 4) return buildNXDomainResponse(query)
        val qEnd = 12
        var pos = 12
        while (pos < query.rawPacket.size && query.rawPacket[pos].toInt() and 0xFF != 0) {
            pos += (query.rawPacket[pos].toInt() and 0xFF) + 1
        }
        pos += 5
        val answerLen = 16
        val total = pos + answerLen
        val buf = ByteBuffer.allocate(total)
        buf.put(query.rawPacket, 0, pos)
        buf.putShort((-16372).toShort()) // 0xC00C pointer to question
        buf.putShort(1)
        buf.putShort(1)
        buf.putInt(60)
        buf.putShort(4)
        parts.forEach { buf.put(it) }
        buf.flip()
        return ByteArray(buf.remaining()).also { buf.get(it) }
    }
}
