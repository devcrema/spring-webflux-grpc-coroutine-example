package com.example.demo

import org.lognet.springboot.grpc.GRpcService
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.lang.Thread.sleep

@GRpcService
class GracefulShutdownTestGrpcService : ReactorVeryLongTimeServiceGrpc.VeryLongTimeServiceImplBase() {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun requestLongTime(request: Mono<GracefulShutdownTest.LongTimeRequest>): Mono<GracefulShutdownTest.LongTimeResponse> =
            request
                    .map {
                        log.info("request received. waiting 20 seconds")
                        sleep(20000)
                        it
                    }
                    .map {
                        log.info("response")
                        GracefulShutdownTest.LongTimeResponse.newBuilder()
                                .setMessage(it.message).build()
                    }
                    .subscribeOn(Schedulers.elastic())
}
