package com.ledger.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EntryType {
    DEBIT,      // money spent
    CREDIT,     // money received
    DUE_OWE,    // you owe someone
    DUE_OWED    // someone owes you
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: EntryType,
    val party: String,
    val note: String = "",
    val rawText: String = "",
    val sourceApp: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    // true once the user has confirmed an auto-parsed entry (or created it manually)
    val reviewed: Boolean = true
)
