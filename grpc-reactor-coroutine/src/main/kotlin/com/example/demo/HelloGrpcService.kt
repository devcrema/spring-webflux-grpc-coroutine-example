package com.example.demo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.mono
import org.lognet.springboot.grpc.GRpcService
import reactor.core.publisher.Mono

@GRpcService
class HelloGrpcService(private val testRepository: TestRepository)
    : ReactorHelloServiceGrpc.HelloServiceImplBase() {

    override fun getHello(request: Mono<Hello.Name>): Mono<Hello.Response> = request
            .also { printThread("request") }
            .flatMap { requestName ->
                mono(SchedulerAndDispatcher.IO_DISPATCHER) {
                    val getSuspend = CoroutineScope(SchedulerAndDispatcher.IO_DISPATCHER).async {
                        printThread("async1")
                        testRepository.findById()
                    }
                    val getFlowAsList = CoroutineScope(SchedulerAndDispatcher.IO_DISPATCHER).async {
                        printThread("async2")
                        testRepository.findAll().toList()
                    }
                    printThread("flatmap")
                    "hello ${requestName.value}\n${getFlowAsList.await()}\n${getSuspend.await()}"
                }
            }
            .map { result ->
                printThread("response map")
                Hello.Response.newBuilder()
                        .setMessage(result)
                        .build()
            }

    private fun printThread(str: String) = println("$str threadName: ${Thread.currentThread().name}")

}
