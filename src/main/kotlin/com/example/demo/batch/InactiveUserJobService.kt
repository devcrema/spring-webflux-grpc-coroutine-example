package com.example.demo.batch

import com.example.demo.batch.InactiveUserJobServiceOuterClass.LaunchInactiveUserJobRequest
import com.example.demo.batch.InactiveUserJobServiceOuterClass.LaunchInactiveUserJobResponse
import org.lognet.springboot.grpc.GRpcService
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@GRpcService
class InactiveUserJobService(private val inactiveUserJobScheduler: InactiveUserJobScheduler) : ReactorInactiveUserJobServiceGrpc.InactiveUserJobServiceImplBase() {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun launchInactiveUserJob(request: Mono<LaunchInactiveUserJobRequest>): Mono<LaunchInactiveUserJobResponse> =
        request.map {
            Mono.fromCallable { inactiveUserJobScheduler.runInactiveUserJob() }
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe { log.info("finished batch job: $it") } // 이렇게 비동기로 해두면 api timeout 시간이 지나더라도 문제가 되지 않는다.

            LaunchInactiveUserJobResponse
                    .newBuilder()
                    .build()
        }

}