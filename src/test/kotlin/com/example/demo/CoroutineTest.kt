package com.example.demo

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletPath
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.lang.Thread.sleep
import java.time.LocalDateTime
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

@Suppress("BlockingMethodInNonBlockingContext")
class CoroutineTest {
    private val log = LoggerFactory.getLogger(this::class.java)

    /*
    17:24:43.292 [Test worker] INFO com.example.demo.CoroutineTest - start mono
    17:24:44.297 [Test worker] INFO com.example.demo.CoroutineTest - monoBuy
    17:24:45.303 [Test worker] INFO com.example.demo.CoroutineTest - monoRefund
    17:24:45.303 [Test worker] INFO com.example.demo.CoroutineTest - success 1000 receipt success
     */
    @Test
    fun `reactor sequence test`() {
        Mono.just(1000)
            .doOnNext { log.info("start mono") }
            .subscribeOn(Schedulers.immediate())
            .flatMap { monoBuy(it) }
            .flatMap { monoRefund(it) }
            .subscribe({
                log.info("success $it")
            }, {
                log.info("failed $it")
            }
            )
        sleep(3000)
    }

    private fun monoBuy(money: Int): Mono<String> = Mono
        .fromCallable { sleep(1000) }
        .map { log.info("monoBuy") }
        .map { "$money receipt" }

    private fun monoRefund(receipt: String): Mono<String> = Mono
        .fromCallable { sleep(1000) }
        .map { log.info("monoRefund") }
        .map { "$receipt success" }

    /*
    17:23:31.683 [Test worker @coroutine#1] INFO com.example.demo.CoroutineTest - start suspend function
    17:23:32.696 [Test worker @coroutine#1] INFO com.example.demo.CoroutineTest - suspendBuy
    17:23:33.702 [Test worker @coroutine#1] INFO com.example.demo.CoroutineTest - suspendRefund
    17:23:33.702 [Test worker @coroutine#1] INFO com.example.demo.CoroutineTest - success 1000 receipt success
    */
    @Test
    fun `coroutine sequence test`() = runBlocking { buyAndRefund() }

    private suspend fun buyAndRefund() {
        log.info("start suspend function")
        val money = 1000
        try {
            val receipt = suspendBuy(money)
            val result = suspendRefund(receipt)
            log.info("success $result")
        } catch (e: Exception) {
            log.info("failed $e")
        }
    }

    private suspend fun suspendBuy(money: Int): String =
        delay(1000)
            .also { log.info("suspendBuy") }
            .let { "$money receipt" }

    private suspend fun suspendRefund(receipt: String): String =
        delay(1000)
            .also { log.info("suspendRefund") }
            .let { "$receipt success" }

    /*
    result
    14:36:12.057 [parallel-1] INFO com.example.demo.CoroutineTest - 1 sleep
    14:36:12.057 [parallel-2] INFO com.example.demo.CoroutineTest - 2 sleep
    14:36:12.057 [parallel-3] INFO com.example.demo.CoroutineTest - 3 sleep
    14:36:13.562 [Test worker] INFO com.example.demo.CoroutineTest - result: [1,2,3]

    if exclude .subscribeOn(Schedulers.parallel())
    14:37:01.360 [Test worker] INFO com.example.demo.CoroutineTest - 1 sleep
    14:37:02.860 [Test worker] INFO com.example.demo.CoroutineTest - 2 sleep
    14:37:04.362 [Test worker] INFO com.example.demo.CoroutineTest - 3 sleep
    14:37:05.874 [Test worker] INFO com.example.demo.CoroutineTest - result: [1,2,3]

    if using Schedulers.elastic()
    14:40:04.098 [elastic-2] INFO com.example.demo.CoroutineTest - 1 sleep
    14:40:04.098 [elastic-4] INFO com.example.demo.CoroutineTest - 3 sleep
    14:40:04.098 [elastic-3] INFO com.example.demo.CoroutineTest - 2 sleep
    14:40:05.606 [Test worker] INFO com.example.demo.CoroutineTest - result: [1,2,3]
     */
    @Test
    fun `reactor concurrency test`() {
        val job1 = Mono.just(1)
            .map {
                log.info("1 sleep")
                sleep(1500)
                it
            }.subscribeOn(Schedulers.parallel())

        val job2 = Mono.just(2)
            .map {
                log.info("2 sleep")
                sleep(1500)
                it
            }.subscribeOn(Schedulers.parallel())

        val job3 = Mono.just(3)
            .map {
                log.info("3 sleep")
                sleep(1500)
                it
            }.subscribeOn(Schedulers.parallel())
        val result = Mono.zip(job1, job2, job3).block()
        log.info("result: $result")
    }

    /*
        result
        14:37:56.339 [DefaultDispatcher-worker-1 @coroutine#2] INFO com.example.demo.CoroutineTest - 1 sleep
        14:37:56.340 [DefaultDispatcher-worker-2 @coroutine#4] INFO com.example.demo.CoroutineTest - 3 sleep
        14:37:56.339 [DefaultDispatcher-worker-3 @coroutine#3] INFO com.example.demo.CoroutineTest - 2 sleep
        14:37:57.850 [Test worker @coroutine#1] INFO com.example.demo.CoroutineTest - 1 2 3

        if using Dispatchers.Unconfined
        14:38:50.002 [Test worker @coroutine#2] INFO com.example.demo.CoroutineTest - 1 sleep
        14:38:51.510 [Test worker @coroutine#3] INFO com.example.demo.CoroutineTest - 2 sleep
        14:38:53.013 [Test worker @coroutine#4] INFO com.example.demo.CoroutineTest - 3 sleep
        14:38:54.516 [Test worker @coroutine#1] INFO com.example.demo.CoroutineTest - 1 2 3

        if using Dispatchers.Unconfined with delay(1500) not sleep(1500)
        14:41:49.597 [Test worker @coroutine#2] INFO com.example.demo.CoroutineTest - 1 sleep
        14:41:49.605 [Test worker @coroutine#3] INFO com.example.demo.CoroutineTest - 2 sleep
        14:41:49.605 [Test worker @coroutine#4] INFO com.example.demo.CoroutineTest - 3 sleep
        14:41:51.112 [Test worker @coroutine#1] INFO com.example.demo.CoroutineTest - 1 2 3
     */
    @Test
    fun `coroutine concurrency test`() = runBlocking { job() }

    private suspend fun job() {
        val job1 = CoroutineScope(Dispatchers.IO).async {
            log.info("1 sleep")
            sleep(1500)
            1
        }
        val job2 = CoroutineScope(Dispatchers.IO).async {
            log.info("2 sleep")
            sleep(1500)
            2
        }
        val job3 = CoroutineScope(Dispatchers.IO).async {
            log.info("3 sleep")
            sleep(1500)
            3
        }
        log.info("${job1.await()} ${job2.await()} ${job3.await()}")
    }

    @Test
    fun `reactor error handling test`() {
        Mono.just("").map {
            throw IllegalStateException("reactor mono error!!")
        }.map {
            "unreached"
        }.onErrorResume {
            Mono.just("$it")
        }.block().also { println("result: $it") }
    }

    @Test
    fun `coroutine error handling test`() {
        val result = runBlocking {
            try {
                throwError()
            } catch (e: Exception) {
                "error $e"
            }
        }
        println("result :$result")
    }

    private suspend fun throwError() {
        delay(10)
        throw IllegalStateException()
    }

    @Test
    fun `coroutine launch scope test`(){
        val singleDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        runBlocking(singleDispatcher){
            launch {
                delay(1000)
                println("1 non blocking ${Thread.currentThread().name}")
                println("2 non blocking ${Thread.currentThread().name}")
            }
            coroutineScope { // suspend block
                println("3 blocking ${Thread.currentThread().name}")
                delay(500)
                println("4 blocking ${Thread.currentThread().name}")
            }
            println("5? ${Thread.currentThread().name}")
        }
    }

    @Test
    fun `coroutine parallel test`() = runBlocking {
        CoroutineScope(Dispatchers.Default).launch {
            (1..5).asFlow()
                .map {
                    this.async {
                        sleep(1000)
                        println(Thread.currentThread().name)
                        it
                    }
                }.buffer(100)
                .collect {
                    println("${Thread.currentThread().name} ${LocalDateTime.now()} : ${it.await()}")
                }
        }.join()
    }

    @Test
    fun `coroutine parallel test2`() = runBlocking {
        CoroutineScope(Dispatchers.Default).launch {
            measureTimeMillis {
                (1..5).asFlow()
                    .concurrentMap(this, 100) {
                        sleep(1000)
                        println(Thread.currentThread().name)
                        it
                    }
                    .concurrentMap(this, 100) {
                        sleep(1000)
                        println(Thread.currentThread().name)
                        it
                    }
                    .collect {
                        println("${Thread.currentThread().name} ${LocalDateTime.now()} : $it")
                    }
            }.also { println("total time : $it") }
        }.join()
    }

    private fun <T, R> Flow<T>.concurrentMap(scope: CoroutineScope, concurrencyLevel: Int, transform: suspend (T) -> R): Flow<R> = this
        .map { scope.async { transform(it) } }
        .buffer(concurrencyLevel)
        .map { it.await() }

    @Test
    fun `coroutine yield test`() = runBlocking {
        val single = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val progress1 = CoroutineScope(single).launch {
            (1..10).forEach {
                println("show progress1 ${it * 10}%")
                yield()
            }
        }
        val progress2 = CoroutineScope(single).launch {
            (1..10).forEach {
                println("show progress2 ${it * 10}%")
                yield()
            }
        }
        progress1.join()
        progress2.join()
    }

    @Test
    fun `coroutine withContext test`() = runBlocking {
        CoroutineScope(Dispatchers.IO).launch {
            val result = withContext(Dispatchers.IO){
                "is this received?"
            }
            println(result)
            CoroutineScope(Dispatchers.IO).launch {
                println("started?")
            }
        }
        Unit
    }

    @Suppress("DeferredResultUnused")
    @Test
    fun `coroutine cancel test`() {
        val job = CoroutineScope(Dispatchers.IO).launch {
            CoroutineScope(Dispatchers.IO).launch {
                sleep(1000)
                println("launch, started")
            }
            CoroutineScope(Dispatchers.IO).async {
                sleep(1000)
                println("async, started")
            }
            withContext(Dispatchers.IO){
                sleep(1000)
                println("with context, started")
            }
            coroutineScope {
                sleep(1000)
                println("coroutineScope, cancelled")
            }
        }
        sleep(500)
        job.cancel()
        sleep(1000)
        println("ends")
    }
}