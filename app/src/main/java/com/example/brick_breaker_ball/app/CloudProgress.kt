package com.example.brick_breaker_ball.app

/** A complete versioned checkpoint. Currency receipts travel with the wallet. */
data class CloudProgressRecord(val revision: Long, val saveId: String, val snapshot: String?, val updatedAt: Long)

data class CloudProgressResult(val record: CloudProgressRecord? = null, val conflict: Boolean = false, val error: String? = null)
