package net.ddns.mlsoftlaberge.trycorder.link

/** Reads the IPv4 address out of a BSD `sockaddr_in` (as returned by Bonjour on iOS in NSNetService.addresses). */
object SockAddr {
    private const val FAMILY_OFFSET = 1
    private const val ADDRESS_OFFSET = 4
    private const val ADDRESS_LENGTH = 4
    private const val AF_INET = 2
    private const val BYTE_MASK = 0xFF

    /** "a.b.c.d" for an AF_INET sockaddr (layout: len, family, port[2], addr[4], zero[8]); null otherwise. */
    fun ipv4(bytes: ByteArray): String? {
        if (bytes.size < ADDRESS_OFFSET + ADDRESS_LENGTH || bytes[FAMILY_OFFSET].toInt() != AF_INET) return null
        return (ADDRESS_OFFSET until ADDRESS_OFFSET + ADDRESS_LENGTH)
            .joinToString(".") { (bytes[it].toInt() and BYTE_MASK).toString() }
    }
}
