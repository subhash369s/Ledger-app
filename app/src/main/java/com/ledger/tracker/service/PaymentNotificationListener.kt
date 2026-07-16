package com.ledger.tracker.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.ledger.tracker.data.AppDatabase
import com.ledger.tracker.data.EntryType
import com.ledger.tracker.data.TransactionEntity
import com.ledger.tracker.parser.TransactionParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Runs system-wide once the user grants Notification Access in Settings.
 * Every notification posted on the device passes through onNotificationPosted.
 * We only act on ones that contain a currency amount -- everything else is ignored.
 */
class PaymentNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Optional: narrow this down to specific package names if you only want
    // to watch certain banking/UPI apps. Left empty = watch everything and
    // rely on the parser (amount must be present) to filter noise.
    private val packageAllowlist: Set<String> = emptySet()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (packageAllowlist.isNotEmpty() && sbn.packageName !in packageAllowlist) return
        // Skip our own notifications to avoid loops.
        if (sbn.packageName == applicationContext.packageName) return

        val extras: android.os.Bundle = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val combined = listOf(title, text, bigText).filter { it.isNotBlank() }.joinToString(" — ")

        if (combined.isBlank()) return

        val parsed = TransactionParser.parse(combined) ?: return
        val amount = parsed.amount ?: return

        scope.launch {
            val dao = AppDatabase.getInstance(applicationContext).transactionDao()
            dao.insert(
                TransactionEntity(
                    amount = amount,
                    // If direction couldn't be determined, default to DEBIT and
                    // let the user fix it in the review queue.
                    type = parsed.type ?: EntryType.DEBIT,
                    party = parsed.party ?: "Unknown",
                    rawText = combined,
                    sourceApp = sbn.packageName,
                    // Auto-captured entries start unreviewed so the user can
                    // confirm or correct them before they count toward totals.
                    reviewed = false
                )
            )
        }
    }
}
