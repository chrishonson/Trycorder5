package net.ddns.mlsoftlaberge.trycorder.link

/**
 * The line protocol spoken with the tryserver (tryserver/tryserver.c) and the other Trycorders.
 * Every message is one text line on TCP port 1701 (Network Common Channel, NCC-1701).
 */
object LinkProtocol {
    const val PORT = 1701

    /** DNS-SD service type announced and browsed on the local network. */
    const val SERVICE_TYPE = "_trycorder._tcp."

    const val TRYCORDERS_TAG = "trycorders:"
    const val STATISTICS_TAG = "statistics:"
    const val LOGS_TAG = "logs:"

    /** First line sent to the tryserver: "trycorder:<device name>/<android release>/<app version>". */
    fun identification(deviceName: String, deviceRelease: String, appVersion: String): String =
        "trycorder:$deviceName/$deviceRelease/$appVersion"

    /**
     * decodetrycorders(): "trycorders:ip,name:ip,name:" is the list of Trycorders connected to the server.
     * Entries without a comma are skipped (the original crashed on them).
     */
    fun parseTrycorders(line: String): List<Peer> =
        line.substringAfter(TRYCORDERS_TAG)
            .split(':')
            .filter { ',' in it }
            .map { Peer(ip = it.substringBefore(',').trim(), name = it.substringAfter(',')) }

    /** decodestatistics(): "statistics:trycorders,countrys,citys,states", unreadable fields count as 0. */
    fun parseStatistics(line: String): ServerStats {
        val fields = line.substringAfter(STATISTICS_TAG).split(',').map { it.trim().toIntOrNull() ?: 0 }
        fun field(index: Int) = fields.getOrElse(index) { 0 }
        return ServerStats(trycorders = field(0), countries = field(1), cities = field(2), states = field(STATES_FIELD))
    }

    /** addiplist(): DNS-SD escapes spaces in service names as "\032". */
    fun cleanServiceName(name: String): String = name.replace("\\032", " ").replace('\\', ' ')

    private const val STATES_FIELD = 3
}
