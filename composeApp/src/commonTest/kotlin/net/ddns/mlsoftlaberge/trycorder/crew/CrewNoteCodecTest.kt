package net.ddns.mlsoftlaberge.trycorder.crew

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.ddns.mlsoftlaberge.trycorder.inventory.InMemoryFileAccessor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrewNoteCodecTest {
    private val original = "Captain of the ship\nLikes tea\n" +
        "ADMIN|2016-03-01 10:20:30|125.50|Uniform repair\n" +
        "ADMIN|20160402|-20|Refund\n"

    @Test
    fun decodeSplitsMemoAndTransactions() {
        val note = CrewNoteCodec.decode(original)
        assertEquals("Captain of the ship\nLikes tea\n", note.memo)
        assertEquals(2, note.transactions.size)
        assertEquals(CrewTransaction("2016-03-01 10:20:30", "125.50", "Uniform repair"), note.transactions[0])
        assertEquals(CrewTransaction("2016-04-02 00:00:00", "-20.00", "Refund"), note.transactions[1])
        assertEquals(105.5, note.total)
    }

    @Test
    fun encodeWritesTheOriginalFormat() {
        val note = CrewNoteCodec.decode(original)
        assertEquals(
            "Captain of the ship\nLikes tea\n" +
                "ADMIN|2016-03-01 10:20:30|125.50|Uniform repair\n" +
                "ADMIN|2016-04-02 00:00:00|-20.00|Refund\n",
            CrewNoteCodec.encode(note),
        )
        // a decoded note encodes back to itself
        assertEquals(CrewNoteCodec.encode(note), CrewNoteCodec.encode(CrewNoteCodec.decode(CrewNoteCodec.encode(note))))
    }

    @Test
    fun memoGetsATrailingNewlineAndLastLineWithoutNewlineIsKept() {
        assertEquals("abc\n", CrewNoteCodec.encode(CrewNote("abc")))
        assertEquals("", CrewNoteCodec.encode(CrewNote("")))
        val note = CrewNoteCodec.decode("memo\nADMIN|2016-01-01|5|last")
        assertEquals("memo\n", note.memo)
        assertEquals("last", note.transactions.single().description)
    }

    @Test
    fun malformedLinesUseTheOriginalDefaults() {
        val partial = CrewNoteCodec.decode("ADMIN|20170101|12\n").transactions.single()
        assertEquals(CrewTransaction("2017-01-01 00:00:00", "12.00", ""), partial)
        val badAmount = CrewNoteCodec.decode("ADMIN|20170101|abc|x\n").transactions.single()
        assertEquals("0.00", badAmount.amount)
        val comma = CrewNoteCodec.decode("ADMIN|20170101|12,5|x\n").transactions.single()
        assertEquals("12.50", comma.amount)
    }

    @Test
    fun dateNormalizationIsLenientLikeCalendar() {
        assertEquals("2016-12-31 23:59:59", TransDate.normalize("2016-12-31 23:59:59"))
        assertEquals("2016-03-01 00:00:00", TransDate.normalize("2016/02/30"))
        assertEquals("2017-01-01 00:00:00", TransDate.normalize("2016-12-31 24:00:00"))
        // day 0 is the last day of the previous month
        assertEquals("2016-02-29 00:00:00", TransDate.normalize("201603"))
        assertEquals("2017-01-01 00:00:00", TransDate.normalize("2016-13-01"))
    }

    @Test
    fun amountsFormatWithTwoDecimals() {
        assertEquals("0.00", Amounts.format(0.0))
        assertEquals("1234.57", Amounts.format(1234.567))
        assertEquals("-0.50", Amounts.format(-0.5))
        assertEquals("0.00", Amounts.format(-0.001))
    }

    @Test
    fun editedTransactionIsNormalized() {
        val t = CrewNoteCodec.transaction("2016-1-2", "", "Two\nlines|pipe")
        assertEquals("0.00", t.amount)
        assertEquals("Two lines/pipe", t.description)
    }

    @Test
    fun invoiceIsAFixedWidthTable() {
        val text = Invoice.build(listOf(CrewTransaction("2016-03-01 10:20:30", "125.50", "Uniform repair")))
        val lines = text.lines()
        assertEquals("| 2016-03-01 10:20:30 |     125.50 | Uniform repair                 |", lines[1])
        assertTrue(lines[3].contains("TOTAL +     125.50 +"))
    }

    @Test
    fun linksAreEncoded() {
        assertEquals("a%20b%26c%C3%A9", UrlCodec.encode("a b&cé"))
        assertEquals("tel:+1418555%231", CrewLinks.phone("+1 (418) 555#1"))
        val mail = CrewLinks.email("jos@chose.com", emptyList())
        assertTrue(mail.startsWith("mailto:jos@chose.com?subject=Account%20State&body=%2B---"))
    }

    @Test
    fun noteStoreKeepsNotesByContactId() = runTest(StandardTestDispatcher()) {
        val file = InMemoryFileAccessor()
        val store = CrewNoteStore(file, StandardTestDispatcher(testScheduler))
        assertEquals("", store.get("abc"))
        assertFalse(store.put("abc", "memo\n"))
        assertTrue(store.put("abc", "memo2\n"))
        val reloaded = CrewNoteStore(file, StandardTestDispatcher(testScheduler))
        assertEquals("memo2\n", reloaded.get("abc"))
        reloaded.put("abc", "")
        assertEquals(emptyMap(), reloaded.all())
    }
}
