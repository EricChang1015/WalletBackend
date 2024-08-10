package com.aspectgaming.wallet.repository

import com.aspectgaming.wallet.model.Account
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.sqlclient.poolOptionsOf
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.Tuple

class AccountRepository(private val vertx: Vertx) {
    private val pool: Pool

    init {
        val connectOptions = PgConnectOptions()
            .setPort(5432)
            .setHost("localhost")
            .setDatabase("your_database")
            .setUser("your_username")
            .setPassword("your_password")

        pool = PgPool.pool(vertx, connectOptions, poolOptionsOf(maxSize = 5))
    }

    fun save(account: Account, handler: Handler<AsyncResult<Account>>) {
        val query = "INSERT INTO accounts (id, user_id, currency, balance) VALUES ($1, $2, $3, $4) RETURNING *"
        val params = Tuple.of(account.id, account.userId, account.currency, account.balance)

        pool.preparedQuery(query)
            .execute(params) { ar ->
                if (ar.succeeded()) {
                    val row = ar.result().first()
                    val savedAccount = Account(
                        id = row.getString("id"),
                        userId = row.getString("user_id"),
                        currency = row.getString("currency"),
                        balance = row.getBigDecimal("balance")
                    )
                    handler.handle(Future.succeededFuture(savedAccount))
                } else {
                    handler.handle(Future.failedFuture(ar.cause()))
                }
            }
    }

    // 實現其他數據庫操作...

    // 如果你想使用協程，可以添加這樣的擴展函數：
    suspend fun saveCoroutine(account: Account): Account {
        val query = "INSERT INTO accounts (id, user_id, currency, balance) VALUES ($1, $2, $3, $4) RETURNING *"
        val params = Tuple.of(account.id, account.userId, account.currency, account.balance)

        val result = pool.preparedQuery(query).execute(params).await()
        val row = result.first()
        return Account(
            id = row.getString("id"),
            userId = row.getString("user_id"),
            currency = row.getString("currency"),
            balance = row.getBigDecimal("balance")
        )
    }
}