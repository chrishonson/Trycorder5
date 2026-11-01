package net.ddns.mlsoftlaberge.trycorder.crew

import kotlin.math.abs
import kotlin.math.roundToLong

/** A transaction line "ADMIN|date|amount|description" of the contact note. */
data class CrewTransaction(val date: String, val amount: String, val description: String) {
    val value: Double get() = Amounts.parse(amount)
}

/** The contact note split like the original: free memo text, and the ADMIN| transaction lines. */
data class CrewNote(val memo: String = "", val transactions: List<CrewTransaction> = emptyList()) {
    val total: Double get() = transactions.sumOf { it.value }
}

/**
 * Reads and writes the format of ContactAdminFragment (expandnote/decodeline/compactnote), so the
 * notes written by the original app stay readable and vice versa.
 */
object CrewNoteCodec {
    const val ADMIN = "ADMIN|"
    private const val DEFAULT_DATE = "20160101"
    private const val DEFAULT_AMOUNT = "0.00"

    fun decode(note: String): CrewNote {
        val memo = StringBuilder()
        val transactions = mutableListOf<CrewTransaction>()
        var i = 0
        while (i < note.length) {
            val nl = note.indexOf('\n', i)
            val end = if (nl < 0) note.length else nl + 1
            val line = note.substring(i, end)
            if (line.startsWith(ADMIN)) transactions += decodeLine(line) else memo.append(line)
            i = end
        }
        return CrewNote(memo.toString(), transactions)
    }

    fun encode(note: CrewNote): String = buildString {
        append(normalizeMemo(note.memo))
        note.transactions.forEach { t ->
            append(ADMIN).append(t.date).append('|').append(t.amount).append('|').append(t.description).append('\n')
        }
    }

    /** A non-empty memo always ends with a newline (normalizememo). */
    fun normalizeMemo(memo: String): String = if (memo.isEmpty() || memo.endsWith('\n')) memo else memo + "\n"

    /** A transaction as the original stored it after editing: formatted amount, normalized date. */
    fun transaction(date: String, amount: String, description: String) = CrewTransaction(
        date = TransDate.normalize(date),
        amount = Amounts.format(Amounts.parse(amount)),
        description = description.replace('\n', ' ').replace('|', '/'),
    )

    private fun decodeLine(line: String): CrewTransaction {
        val text = line.removeSuffix("\n")
        val parts = text.removePrefix(ADMIN).split('|')
        val date = parts.getOrElse(0) { DEFAULT_DATE }
        val amount = parts.getOrElse(1) { DEFAULT_AMOUNT }
        val description = if (parts.size == 1) text else parts.getOrElse(2) { "" }
        return CrewTransaction(TransDate.normalize(date), Amounts.format(Amounts.parse(amount)), description)
    }
}

/** Amounts as "%.2f". Parsing also accepts a decimal comma (the original formatted with the device locale). */
object Amounts {
    fun parse(text: String): Double = text.trim().replace(',', '.').toDoubleOrNull() ?: 0.0

    fun format(value: Double): String {
        val cents = (abs(value) * CENTS).roundToLong()
        val sign = if (value < 0 && cents != 0L) "-" else ""
        return "$sign${cents / CENTS}.${(cents % CENTS).toString().padStart(2, '0')}"
    }

    private const val CENTS = 100L
}

/**
 * The lenient date normalization of stringToDate + dateToString: keeps the digits, reads
 * yyyyMMddHHmmss with the same defaults, lets out-of-range fields overflow like Calendar,
 * and formats "yyyy-MM-dd HH:mm:ss".
 */
@Suppress("MagicNumber") // calendar arithmetic and field positions
object TransDate {
    fun normalize(text: String): String {
        val digits = text.filter { it in '0'..'9' }.take(14)
        fun field(from: Int, to: Int, default: Int) =
            if (digits.length >= to) digits.substring(from, to).toInt() else default
        val seconds = (field(8, 10, 0) * 60L + field(10, 12, 0)) * 60L + field(12, 14, 0)
        val months = field(4, 6, 1) - 1L
        val year = field(0, 4, 0) + months.floorDiv(12L)
        val month = months.mod(12L).toInt() + 1
        val day = daysFromCivil(year, month, 1) + field(6, 8, 0) - 1 + seconds.floorDiv(86_400L)
        val (y, m, d) = civilFromDays(day)
        val s = seconds.mod(86_400L)
        return "${pad(y, 4)}-${pad(m.toLong(), 2)}-${pad(d.toLong(), 2)} " +
            "${pad(s / 3600, 2)}:${pad(s / 60 % 60, 2)}:${pad(s % 60, 2)}"
    }

    private fun pad(v: Long, width: Int) = v.toString().padStart(width, '0')

    private fun daysFromCivil(year: Long, month: Int, day: Int): Long {
        val y = if (month <= 2) year - 1 else year
        val era = y.floorDiv(400L)
        val yoe = y - era * 400
        val mp = (month + 9) % 12
        val doy = (153 * mp + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146_097 + doe - 719_468
    }

    private fun civilFromDays(days: Long): Triple<Long, Int, Int> {
        val z = days + 719_468
        val era = z.floorDiv(146_097L)
        val doe = z - era * 146_097
        val yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = (doy - (153 * mp + 2) / 5 + 1).toInt()
        val m = (if (mp < 10) mp + 3 else mp - 9).toInt()
        val y = yoe + era * 400
        return Triple(if (m <= 2) y + 1 else y, m, d)
    }
}

/** prepare_invoice: the account statement sent by email, as a fixed-width text table. */
object Invoice {
    private const val RULE = "+-----------+---------+------------+-------------------------------+\n"
    private const val AMOUNT_WIDTH = 10
    private const val DESCRIPTION_WIDTH = 30

    fun build(transactions: List<CrewTransaction>): String = buildString {
        append(RULE)
        transactions.forEach { t ->
            append("| ").append(t.date).append(" | ")
            append(Amounts.format(t.value).padStart(AMOUNT_WIDTH)).append(" | ")
            append(t.description.take(DESCRIPTION_WIDTH).padEnd(DESCRIPTION_WIDTH)).append(" |\n")
        }
        append(RULE)
        val total = Amounts.format(transactions.sumOf { it.value }).padStart(AMOUNT_WIDTH)
        append("+               TOTAL + ").append(total).append(" +                               +\n")
        append(RULE)
    }
}

/** Percent-encoding for tel:, mailto: and maps URLs. */
object UrlCodec {
    private const val HEX = "0123456789ABCDEF"
    private const val UNRESERVED = "-_.~"
    private const val BYTE_MASK = 0xFF
    private const val NIBBLE = 4
    private const val LOW_MASK = 0x0F

    fun encode(text: String): String = buildString {
        text.encodeToByteArray().forEach { b ->
            val c = b.toInt() and BYTE_MASK
            val ch = c.toChar()
            if (c < BYTE_MASK / 2 && (ch.isLetterOrDigit() || ch in UNRESERVED)) {
                append(ch)
            } else {
                append('%').append(HEX[c shr NIBBLE]).append(HEX[c and LOW_MASK])
            }
        }
    }
}
