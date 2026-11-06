package dev.yas.ledger.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        RawSmsEntity::class,
        AccountEntity::class,
        AccountIdentifierEntity::class,
        TransactionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class LedgerDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
}
