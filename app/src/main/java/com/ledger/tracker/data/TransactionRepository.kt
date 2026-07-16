package com.ledger.tracker.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class TransactionRepository(context: Context) {
    private val dao = AppDatabase.getInstance(context).transactionDao()

    fun observeAll(): Flow<List<TransactionEntity>> = dao.observeAll()
    fun observeUnreviewed(): Flow<List<TransactionEntity>> = dao.observeUnreviewed()

    suspend fun add(entity: TransactionEntity) = dao.insert(entity)
    suspend fun update(entity: TransactionEntity) = dao.update(entity)
    suspend fun delete(entity: TransactionEntity) = dao.delete(entity)
}
