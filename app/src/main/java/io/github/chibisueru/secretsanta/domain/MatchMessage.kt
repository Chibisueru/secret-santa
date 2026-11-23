package io.github.chibisueru.secretsanta.domain

import io.github.chibisueru.secretsanta.data.GroupEntity

object MatchMessage {

    fun subject(group: GroupEntity): String = "Your Secret Santa match for ${group.name} 🎅"

    fun body(group: GroupEntity, giverName: String, receiverName: String): String = buildString {
        appendLine("Hi $giverName! 🎅")
        appendLine()
        appendLine("You're the Secret Santa for: ${NameCode.encodeForMessage(receiverName)} 🎁")
        appendLine()
        appendLine(
            "🔒 The name is encoded so nobody else can see it. Decode it in the Secret Santa app's " +
                "Decoder tab, or with any Base64 decoder.",
        )
        appendLine()
        appendLine("🎄 ${group.name}")
        if (group.budget.isNotBlank()) appendLine("💰 Budget: ${group.budget.trim()}")
        group.eventDateMillis?.let { appendLine("📅 Gift exchange: ${formatEventDate(it)}") }
        if (group.location.isNotBlank()) appendLine("📍 Location: ${group.location.trim()}")
        if (group.notes.isNotBlank()) {
            appendLine()
            appendLine(group.notes.trim())
        }
        appendLine()
        append("Shh, keep it a secret! 🤫")
        if (group.organizerName.isNotBlank()) {
            appendLine()
            append("Organized by ${group.organizerName.trim()}")
        }
    }
}
