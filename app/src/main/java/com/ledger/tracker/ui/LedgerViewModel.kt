package com.ledger.tracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.tracker.data.EntryType
import com.ledger.tracker.data.TransactionEntity
import com.ledger.tracker.data.TransactionRepository
import com.ledger.tracker.parser.TransactionParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class Totals(
    val debit: Double = 0.0,
    val credit: Double = 0.0,
    val dueOwe: Double = 0.0,
    val dueOwed: Double = 0.0
) {
    val balance get() = credit - debit
    val netDue get() = dueOwed - dueOwe
}

data class LedgerUiState(
    val entries: List<TransactionEntity> = emptyList(),
    val unreviewed: List<TransactionEntity> = emptyList(),
    val totals: Totals = Totals()
)

class LedgerViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = TransactionRepository(application)

    val uiState: StateFlow<LedgerUiState> = combine(
        repo.observeAll(),
        repo.observeUnreviewed()
    ) { all, unreviewed ->
        val totals = Totals(
            debit = all.filter { it.type == EntryType.DEBIT }.sumOf { it.amount },
            credit = all.filter { it.type == EntryType.CREDIT }.sumOf { it.amount },
            dueOwe = all.filter { it.type == EntryType.DUE_OWE }.sumOf { it.amount },
            dueOwed = all.filter { it.type == EntryType.DUE_OWED }.sumOf { it.amount }
        )
        LedgerUiState(entries = all, unreviewed = unreviewed, totals = totals)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LedgerUiState())

    /** Parses free-typed/pasted text; returns a draft for the UI to show for confirmation. */
    fun parseDraft(text: String): TransactionEntity {
        val parsed = TransactionParser.parse(text)
        return TransactionEntity(
            amount = parsed?.amount ?: 0.0,
            type = parsed?.type ?: EntryType.DEBIT,
            party = parsed?.party ?: "",
            rawText = text,
            reviewed = true
        )
    }

    fun save(entity: TransactionEntity) = viewModelScope.launch {
        if (entity.id == 0L) repo.add(entity.copy(reviewed = true))
        else repo.update(entity.copy(reviewed = true))
    }

    fun delete(entity: TransactionEntity) = viewModelScope.launch {
        repo.delete(entity)
    }
}
