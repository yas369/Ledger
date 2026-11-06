package dev.yas.ledger.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class TransactionWithAccount(
    @Embedded val transaction: TransactionEntity,
    val accountName: String?,
)

@Dao
interface AccountDao {
    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Query("SELECT * FROM accounts WHERE ownership = 'MINE' ORDER BY displayName")
    fun observeMine(): Flow<List<AccountEntity>>
}

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Query(
        """
        SELECT t.*, a.displayName AS accountName FROM transactions t
        LEFT JOIN accounts a ON a.id = t.accountId
        WHERE t.deleted = 0
        ORDER BY t.occurredAt DESC
        LIMIT :limit
        """,
    )
    fun observeRecent(limit: Int): Flow<List<TransactionWithAccount>>

    /**
     * Money going out in [from, to). Only plain debits count: credits, self-transfers, lent
     * money and repayments never do. Manual entries with no account count; entries on an
     * account the user has not confirmed as theirs do not.
     */
    @Query(
        """
        SELECT COALESCE(SUM(amountPaise), 0) FROM transactions
        WHERE type = 'DEBIT' AND deleted = 0
          AND occurredAt >= :from AND occurredAt < :to
          AND (accountId IS NULL OR accountId IN (SELECT id FROM accounts WHERE ownership = 'MINE'))
        """,
    )
    fun observeSpent(from: Long, to: Long): Flow<Long>
}
