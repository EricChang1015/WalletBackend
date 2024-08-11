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
            .setDatabase("wallet_db")
            .setUser("wallet_user")
            .setPassword("wallet_pass")

        pool = PgPool.pool(vertx, connectOptions, poolOptionsOf(maxSize = 5))
    }

    fun save(account: Account, handler: Handler<AsyncResult<Account>>) {
        val query = "INSERT INTO wallets (user_id, balance) VALUES ($1, $2) RETURNING *"
        //        val query = "INSERT INTO accounts (id, user_id, currency, balance) VALUES ($1, $2, $3, $4) RETURNING *"
        val params = Tuple.of(account.userId.toInt(), account.balance)

        pool.preparedQuery(query)
            .execute(params) { ar ->
                if (ar.succeeded()) {
                    println("Saved account: ${ar.result().size()}")
                    val row = ar.result().first()
                    val savedAccount = Account(
                        id = row.getInteger("id").toString(),
                        userId = row.getInteger("user_id"),
                        balance = row.getBigDecimal("balance")
                    )
                    handler.handle(Future.succeededFuture(savedAccount))
                } else {
                    println("Failed to save account: ${ar.cause().message}")
                    handler.handle(Future.failedFuture(ar.cause()))
                }
            }
    }
}