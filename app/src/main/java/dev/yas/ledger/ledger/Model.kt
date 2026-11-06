package dev.yas.ledger.ledger

enum class AccountKind { BANK_ACCOUNT, CARD, WALLET }

enum class Ownership { MINE, NOT_MINE, UNCONFIRMED }

enum class Direction { DEBIT, CREDIT }

enum class TxType { DEBIT, CREDIT, SELF_TRANSFER, LENT, REPAYMENT }

enum class TxSource { SMS, MANUAL, MERGED }

enum class RawSmsStatus { TRANSACTION, LABEL, IGNORED, UNREADABLE, PENDING }

enum class IgnoreReason {
    OTP,
    NOT_COMPLETED,
    BALANCE_ONLY,
    PROMO,
    NO_ACCOUNT,
    NOT_MINE,
    USER_MARKED,
    DUPLICATE,
}

/** An account as the UI sees it. Balance is the last one a bank SMS reported, never a live figure. */
data class Account(
    val id: Long,
    val kind: AccountKind,
    val displayName: String,
    val balancePaise: Long?,
    val balanceAsOf: Long?,
)

/** What the Today screen needs to know about the user's accounts. Cards are never listed. */
data class AccountsSummary(
    val banks: List<Account>,
    val wallets: List<Account>,
    val totalBalancePaise: Long?,
    val knownBalanceCount: Int,
) {
    val bankCount: Int get() = banks.size
}

data class LedgerTransaction(
    val id: Long,
    val accountName: String?,
    val amountPaise: Long,
    val direction: Direction,
    val occurredAt: Long,
    val note: String?,
)
