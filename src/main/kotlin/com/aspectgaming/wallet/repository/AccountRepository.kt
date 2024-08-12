package com.aspectgaming.wallet.repository

import com.aspectgaming.wallet.model.Account
import com.aspectgaming.wallet.model.User
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
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

    fun newUser(user: User, handler: Handler<AsyncResult<User>>) {
        println("Saving user: $user")
        val query = "INSERT INTO users (username, password, email) VALUES ($1, $2, $3) RETURNING *"
        val params = Tuple.of(user.username, user.password, user.email)

        pool.preparedQuery(query)
            .execute(params) { ar ->
                if (ar.succeeded()) {
                    println("User saved: $user")
                    val row = ar.result().first()
                    val savedUser = User(
                        username = row.getString("username"),
                        password = row.getString("password"),
                        email = row.getString("email")
                    )
                    handler.handle(Future.succeededFuture(savedUser))
                } else {
                    // print backtrace
                    ar.cause().printStackTrace()
                    println("Failed to save user: $user - ${ar.cause()}")
                    handler.handle(Future.failedFuture(ar.cause()))
                }
            }
    }

    fun newAccount(account: Account, handler: Handler<AsyncResult<Account>>) {
        val query = "INSERT INTO accounts (user_id, currency) VALUES ($1, $2) RETURNING *"
        val params = Tuple.of(account.userId, account.currency)

        pool.preparedQuery(query)
            .execute(params) { ar ->
                if (ar.succeeded()) {
                    val row = ar.result().first()
                    val savedAccount = Account(
                        userId = row.getString("user_id"),
                        currency = row.getString("currency")
                    )
                    handler.handle(Future.succeededFuture(savedAccount))
                } else {
                    handler.handle(Future.failedFuture(ar.cause()))
                }
            }
    }


}