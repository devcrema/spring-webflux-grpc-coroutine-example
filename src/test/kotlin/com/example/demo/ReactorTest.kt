package com.example.demo

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.lang.RuntimeException
import java.lang.Thread.sleep
import java.time.Duration

@Suppress("BlockingMethodInNonBlockingContext")
class ReactorTest {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Test
    fun reactorSchedulersTest() {
        val subA = Schedulers.newElastic("subA")
        val subB = Schedulers.newElastic("subB")
        val subC = Schedulers.newElastic("subC")
        val pubA = Schedulers.newElastic("pubA")
        val pubB = Schedulers.newElastic("pubB")
        val pubC = Schedulers.newElastic("pubC")
        Mono.just(1)
                .map { it.logThis() } // subA
                .subscribeOn(subA)
                .flatMap {
                    Mono.just(2)
                            .map { it.logThis() } // subB
                            .subscribeOn(subB)
                            .publishOn(subA)
                }
                .map{3.logThis()} // subA
                .subscribeOn(subB) // subA, not subB
                .map{4.logThis()} // subA, not subB
                .publishOn(pubA) // pubA
                .map{5.logThis()} // pubA
                .subscribeOn(subC) // pubA, not subC
                .map{6.logThis()} // pubA, not subC
                .publishOn(pubB) // pubB
                .map{7.logThis()} // pubB
                .publishOn(pubC) //pubC
                .map{8.logThis()} // pubC
                .block()
    }

    @Test
    fun reactorZipTest() {
        log.info("start")
        val sub1 = Schedulers.newElastic("sub1")
        val sub2 = Schedulers.newBoundedElastic(1, 2, "sub2")
        Mono.zip(
                Mono.just(1)
                        .map {
                            sleep(1000)
                            it.logThis()
                        }.subscribeOn(sub1),
                Mono.just(2)
                        .map {
                            sleep(2000)
                            it.logThis()
                        }.subscribeOn(sub2),
                Mono.just(3)
                        .map {
                            sleep(1000)
                            it.logThis()
                        }.subscribeOn(sub2)
        )
                .map { it.t1 + it.t2 + it.t3 }
                .block().also { it?.logThis() } // result in 3000 (1000 + 2000) millis.
    }

    @Test
    fun reactorTimeoutRetryTest(){
        Mono.just(1)
                .flatMap {
                    log.info("wait...")
                    Mono.just(2)
                            .map { sleep(2000) }
                            .subscribeOn(Schedulers.newElastic("2")) // with same thread. can not timeout catch.
                }
                /**
                 * must no signal in duration. if some signal received then no timeout exception occur.
                 * if timeout in same thread, timeout start to catch after sleep 2000. so does not throw any timeout exception.
                 * because stream is sequentially triggered. Mono.just(2).map{sleep(2000)} -> timeout(1000) -> already received signal -> no timeout
                 */
                .timeout(Duration.ofMillis(1000), Mono.error(NullPointerException())) // fallback to custom error
                .retry(2) // on error signal received then retry.
//                .timeout(Duration.ofMillis(1000)) // java.util.concurrent.TimeoutException
                .subscribeOn(Schedulers.newElastic("1"))
                .onErrorResume {
                    log.info(it.message)
                    Mono.empty()
                }
                .block()
    }

    @Test
    fun reactorThreadTest() {
        val sub1 = Schedulers.newElastic("sub1")
        val sub2 = Schedulers.newElastic("sub2")
        mainThread = Thread.currentThread()

        Mono.just(1)
                .map {
                    thread1 = Thread.currentThread()
                    logThread() // 1 runnable, main waiting
                    it.logThis()
                }.subscribeOn(sub1)
                .flatMap {
                    Mono.just(2)
                            .map {
                                thread2 = Thread.currentThread()
                                logThread() // 1, 2 runnable, main waiting
                            }.map {
                                logThread() // 1 waiting, 2 runnable, main waiting
                                it
                            }
                            .subscribeOn(sub2)
                            .publishOn(sub1)
                }.map {
                    logThread() // 1 runnable, 2 waiting, main waiting
                }.block()
        logThread() // 1 runnable, 2 waiting, main runnable
    }

    companion object{
        var thread1: Thread? = null
        var thread2: Thread? = null
        var mainThread: Thread? = null
    }

    private fun logThread() = "thread1".also { thread1?.logThis(it) }
            .also { "thread2".also { thread2?.logThis(it) } }
            .also { "mainThread".also { mainThread?.logThis(it) } }

    private fun Thread.logThis(name: String = "") = this.also { log.info(name + " " + this.state.toString()) }

    private fun Int.logThis() = this.also { log.info(it.toString()) }

}