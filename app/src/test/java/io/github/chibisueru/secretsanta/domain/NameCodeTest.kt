package io.github.chibisueru.secretsanta.domain

import io.github.chibisueru.secretsanta.data.GroupEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NameCodeTest {

    @Test
    fun encodesUtf8Names() {
        assertEquals("TcOhdMOp", NameCode.encode("Máté"))
        assertEquals("[TcOhdMOp]", NameCode.encodeForMessage(" Máté "))
    }

    @Test
    fun roundTrips() {
        listOf("Máté", "Csani", "Zoë-Ann O'Neil", "李雷", "Anna 🎄").forEach {
            assertEquals(it, NameCode.decode(NameCode.encode(it)))
        }
    }

    @Test
    fun decodesLooseInput() {
        assertEquals("Máté", NameCode.decodeInput("  TcOhdMOp \n"))
        assertEquals("Máté", NameCode.decodeInput("[TcOhdMOp]"))
        // Missing padding: "Anna" is "QW5uYQ==".
        assertEquals("Anna", NameCode.decodeInput("QW5uYQ"))
    }

    @Test
    fun findsCodeInWholeMessage() {
        val group = GroupEntity(name = "Family", budget = "\$30", organizerName = "Csani")
        val message = MatchMessage.body(group, "Csani", "Máté")
        assertFalse("Plain name must not appear", message.contains("Máté"))
        assertTrue(message.contains("[TcOhdMOp]"))
        assertEquals("Máté", NameCode.decodeInput(message))
    }

    @Test
    fun rejectsGarbage() {
        assertNull(NameCode.decodeInput(""))
        assertNull(NameCode.decodeInput("hello world!"))
        assertNull(NameCode.decodeInput("/w=="))   // decodes to an invalid UTF-8 byte
        assertNull(NameCode.decodeInput("A"))
    }
}
