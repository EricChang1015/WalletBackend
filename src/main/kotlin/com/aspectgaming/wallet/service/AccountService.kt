package com.aspectgaming.wallet.service

import com.aspectgaming.wallet.model.Account
import com.aspectgaming.wallet.repository.AccountRepository
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.redis.client.Redis
import io.vertx.redis.client.RedisAPI
import io.vertx.redis.client.RedisOptions

// 賬戶服務
class AccountService(private val vertx: Vertx) {
    private val accountRepository = AccountRepository(vertx)
    private val redisClient: Redis = Redis.createClient(vertx, RedisOptions())
    private val redisAPI: RedisAPI = RedisAPI.api(redisClient)

    fun createAccount(account: Account, handler: Handler<AsyncResult<Account>>) {
        accountRepository.save(account) { ar ->
            if (ar.succeeded()) {
                // 將賬戶信息緩存到Redis
                val accountJson = JsonObject.mapFrom(account).encode()
                redisAPI.set(listOf("account:${account.id}", accountJson)) { redisResult ->
                    if (redisResult.succeeded()) {
                        handler.handle(Future.succeededFuture(ar.result()))
                    } else {
                        // 處理Redis寫入失敗的情況
                        handler.handle(Future.failedFuture(redisResult.cause()))
                    }
                }
            } else {
                handler.handle(Future.failedFuture(ar.cause()))
            }
        }
    }
}