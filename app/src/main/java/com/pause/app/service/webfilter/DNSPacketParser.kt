package com.pause.app.service.webfilter

import java.nio.ByteBuffer

/**
 * Parses DNS packets and extracts the queried domain name.
 * Builds NXDOMAIN and redirect responses.
 */
object DNSPacketParser {

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
