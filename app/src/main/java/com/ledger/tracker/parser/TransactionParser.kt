package com.ledger.tracker.parser

import com.ledger.tracker.data.EntryType

data class ParsedTransaction(
    val amount: Double?,
    val type: EntryType?,
    val party: String?,
    val rawText: String
)

object TransactionParser {

    private val amountRegex = Regex(
        """(?:rs\.?|inr|₹)\s?([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )
    private val debitWords = Regex(
        """debited|paid|sent|spent|withdrawn|purchase|debit of""",
        RegexOption.IGNORE_CASE
    )
    private val creditWords = Regex(
        """credited|received|deposited|credit of""",
        RegexOption.IGNORE_CASE
    )
    private val toRegex = Regex(
        """\bto\s+([A-Za-z0-9@.\-_&' ]{2,40}?)(?:\s+on\b|\s+via\b|\s+ref\b|[.,\n]|$)""",
        RegexOption.IGNORE_CASE
    )
    private val fromRegex = Regex(
        """\bfrom\s+([A-Za-z0-9@.\-_&' ]{2,40}?)(?:\s+on\b|\s+via\b|\s+ref\b|[.,\n]|$)""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Returns null if the text doesn't look like a financial notification at all
     * (no currency amount found) so callers can skip non-payment notifications.
     */
    fun parse(text: String): ParsedTransaction? {
        val amountMatch = amountRegex.find(text) ?: return null
        val amount = amountMatch.groupValues[1].replace(",", "").toDoubleOrNull()

        val type = when {
            debitWords.containsMatchIn(text) -> EntryType.DEBIT
            creditWords.containsMatchIn(text) -> EntryType.CREDIT
            else -> null
        }

        val toMatch = toRegex.find(text)?.groupValues?.get(1)?.trim()
        val fromMatch = fromRegex.find(text)?.groupValues?.get(1)?.trim()

        val party = when (type) {
            EntryType.DEBIT -> toMatch ?: fromMatch
            EntryType.CREDIT -> fromMatch ?: toMatch
            else -> toMatch ?: fromMatch
        }

        return ParsedTransaction(amount, type, party, text)
    }
}
