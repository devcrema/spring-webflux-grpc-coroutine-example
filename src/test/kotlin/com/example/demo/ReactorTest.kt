package com.example.demo

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
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
                    Mono.just(2.logThis())
                            .map { it.logThis() } // subB
                            .subscribeOn(subB)
                            .publishOn(subA) // if no publishOn, next map subscribeOn subB, not subA
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
                }.log()
                /**
                 * must no signal in duration. if some signal received then no timeout exception occur.
                 * if timeout in same thread,
                 * called to register asynchronously subscribe at almost sametime
                 * then timeout received onNext as Unit.
                 * anyway, timeout has signal in it, so it doesn't throw..
                 * 01:09:23.969 [1-2] INFO reactor.Mono.FlatMap.1 - | onSubscribe([Fuseable] FluxMapFuseable.MapFuseableSubscriber)
                 * 01:09:23.972 [1-2] INFO reactor.Mono.Timeout.2 - onSubscribe(SerializedSubscriber)
                 * 01:09:23.973 [1-2] INFO reactor.Mono.Timeout.2 - request(unbounded)
                 * 01:09:23.973 [1-2] INFO reactor.Mono.FlatMap.1 - | request(unbounded)
                 * 01:09:25.979 [1-2] INFO reactor.Mono.FlatMap.1 - | onNext(kotlin.Unit)
                 * 01:09:25.979 [1-2] INFO reactor.Mono.Timeout.2 - onNext(kotlin.Unit)
                 * 01:09:25.984 [1-2] INFO reactor.Mono.FlatMap.1 - | onComplete()
                 * 01:09:25.984 [1-2] INFO reactor.Mono.Timeout.2 - onComplete()
                 */
                .timeout(Duration.ofMillis(1000), Mono.error(NullPointerException())).log() // fallback to custom error
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

        Mono.just("thread reuse test") // reuse already exist waiting thread. sub1.
                .map { logThread() } // 1 runnable, 2 waiting, main waiting
                .subscribeOn(sub1)
                .block()
    }

    @Test
    fun reactorAsyncUnitCallTest(){
        Mono.just("run 1")
                .map { sleep(1000)
                it}
                .subscribeOn(Schedulers.boundedElastic()).log()
                .subscribe { log.info(it) }
//                .dispose() // when dispose this, it cancelled
        // not dispose this, onNext once and onComplete()
        // 18:31:49.594 [boundedElastic-1] INFO reactor.Mono.SubscribeOn.1 - onNext(run 1)
        // 18:31:49.594 [boundedElastic-1] INFO com.example.demo.ReactorTest - run 1
        // 18:31:49.595 [boundedElastic-1] INFO reactor.Mono.SubscribeOn.1 - onComplete()
        sleep(2000)
    }

    @Test
    fun reactorFromCallableTest(){
        Mono.fromCallable { testLogging() }.log() // started at subscribeOn thread
                .subscribeOn(Schedulers.boundedElastic()).log()
                .subscribe { log.info("end") }

        sleep(1000)
    }

    companion object{
        var thread1: Thread? = null
        var thread2: Thread? = null
        var mainThread: Thread? = null
    }

    private fun testLogging() = "test1".also { log.info(it) }

    private fun logThread() = "thread1".also { thread1?.logThis(it) }
            .also { "thread2".also { thread2?.logThis(it) } }
            .also { "mainThread".also { mainThread?.logThis(it) } }

    private fun Thread.logThis(name: String = "") = this.also { log.info(name + " " + this.state.toString()) }

    private fun Int.logThis() = this.also { log.info(it.toString()) }

}