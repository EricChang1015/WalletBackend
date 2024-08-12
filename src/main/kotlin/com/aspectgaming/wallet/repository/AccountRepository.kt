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
import java.math.BigDecimal

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
        val query = "INSERT INTO accounts (id, user_id, currency, balance) VALUES ($1, $2, $3, $4) RETURNING *"
        val params = Tuple.of(account.id, account.userId, account.currency, account.balance)

        pool.preparedQuery(query)
            .execute(params) { ar ->
                if (ar.succeeded()) {
                    val row = ar.result().first()
                    val savedAccount = Account(
                        id = row.getString("id"),
                        userId = row.getInteger("user_id"),
                        currency = row.getString("currency"),
                        balance = row.getBigDecimal("balance")
                    )
                    handler.handle(Future.succeededFuture(savedAccount))
                } else {
                    handler.handle(Future.failedFuture(ar.cause()))
                }
            }
    }

    fun findById(id: String, handler: Handler<AsyncResult<Account?>>) {
        val query = "SELECT * FROM accounts WHERE id = $1"
        val params = Tuple.of(id)

        pool.preparedQuery(query)
            .execute(params) { ar ->
                if (ar.succeeded()) {
                    val row = ar.result().firstOrNull()
                    if (row != null) {
                        val account = Account(
                            id = row.getString("id"),
                            userId = row.getInteger("user_id"),
                            currency = row.getString("currency"),
                            balance = row.getBigDecimal("balance")
                        )
                        handler.handle(Future.succeededFuture(account))
                    } else {
                        handler.handle(Future.succeededFuture(null))
                    }
                } else {
                    handler.handle(Future.failedFuture(ar.cause()))
                }
            }
    }

    fun updateBalance(id: String, amount: BigDecimal, handler: Handler<AsyncResult<Account>>) {
        val query = "UPDATE accounts SET balance = balance + $1 WHERE id = $2 RETURNING *"
        val params = Tuple.of(amount, id)

        pool.preparedQuery(query)
            .execute(params) { ar ->
                if (ar.succeeded()) {
                    val row = ar.result().first()
                    val updatedAccount = Account(
                        id = row.getString("id"),
                        userId = row.getInteger("user_id"),
                        currency = row.getString("currency"),
                        balance = row.getBigDecimal("balance")
                    )
                    handler.handle(Future.succeededFuture(updatedAccount))
                } else {
                    handler.handle(Future.failedFuture(ar.cause()))
                }
            }
    }
}