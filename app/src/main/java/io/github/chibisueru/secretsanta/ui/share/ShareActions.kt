package io.github.chibisueru.secretsanta.ui.share

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri

enum class ShareMethod { SMS, EMAIL, OTHER }

fun preferredMethod(phone: String, email: String): ShareMethod = when {
    phone.isNotBlank() -> ShareMethod.SMS
    email.isNotBlank() -> ShareMethod.EMAIL
    else -> ShareMethod.OTHER
}

/**
 * Opens the chosen app with the message filled in. Falls back to the share sheet when
 * there's no SMS or email app. Returns false only if nothing could be opened.
 */
fun sendMatch(
    context: Context,
    method: ShareMethod,
    recipientName: String,
    phone: String,
    email: String,
    subject: String,
    body: String,
): Boolean {
    val direct: Intent? = when (method) {
        ShareMethod.SMS -> Intent(Intent.ACTION_SENDTO, ("smsto:" + Uri.encode(phone.trim())).toUri())
            .putExtra("sms_body", body)
        ShareMethod.EMAIL -> Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
            .putExtra(Intent.EXTRA_EMAIL, arrayOf(email.trim()))
            .putExtra(Intent.EXTRA_SUBJECT, subject)
            .putExtra(Intent.EXTRA_TEXT, body)
        ShareMethod.OTHER -> null
    }

    if (direct != null) {
        try {
            context.startActivity(direct)
            return true
        } catch (_: ActivityNotFoundException) {
            // No SMS or email app: use the share sheet instead.
        }
    }

    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, subject)
        .putExtra(Intent.EXTRA_TEXT, body)
    return try {
        context.startActivity(Intent.createChooser(send, "Send $recipientName their match"))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
