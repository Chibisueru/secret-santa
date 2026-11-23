package io.github.chibisueru.secretsanta.domain

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.util.Base64

/**
 * Encodes the recipient's name as Base64 (UTF-8) so whoever sends a message can't read it
 * at a glance. "Máté" becomes "TcOhdMOp".
 */
object NameCode {

    /** Matches the bracketed code in a match message, e.g. "[TcOhdMOp]". */
    private val BRACKETED = Regex("""\[\s*([A-Za-z0-9+/_\-]+={0,2})\s*]""")

    fun encode(name: String): String =
        Base64.getEncoder().encodeToString(name.trim().toByteArray(Charsets.UTF_8))

    /** Wraps the code in brackets so the decoder can find it inside a whole pasted message. */
    fun encodeForMessage(name: String): String = "[${encode(name)}]"

    /**
     * Decodes a code, or finds and decodes the bracketed code in a whole pasted message.
     * Returns null if no valid name is found.
     */
    fun decodeInput(input: String): String? {
        BRACKETED.findAll(input).forEach { match ->
            decode(match.groupValues[1])?.let { return it }
        }
        return decode(input)
    }

    fun decode(code: String): String? {
        // Accept URL-safe characters and missing padding too.
        val cleaned = code.filterNot { it.isWhitespace() }
            .replace('-', '+')
            .replace('_', '/')
            .trimEnd('=')
        if (cleaned.isEmpty() || cleaned.length % 4 == 1) return null
        if (!cleaned.all { it.isLetterOrDigit() && it.code < 128 || it == '+' || it == '/' }) return null
        val padded = cleaned + "=".repeat((4 - cleaned.length % 4) % 4)

        val bytes = try {
            Base64.getDecoder().decode(padded)
        } catch (_: IllegalArgumentException) {
            return null
        }
        val text = try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (_: CharacterCodingException) {
            return null
        }
        return text.takeIf { it.isNotBlank() && it.none(Char::isISOControl) }
    }
}
