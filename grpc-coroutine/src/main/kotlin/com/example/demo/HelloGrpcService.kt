package com.example.demo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.toList
import org.lognet.springboot.grpc.GRpcService

@GRpcService
class HelloGrpcService(private val testRepository: TestRepository) :
    HelloServiceGrpcKt.HelloServiceCoroutineImplBase(SchedulerAndDispatcher.IO_DISPATCHER) {

    override suspend fun getHello(request: Hello.Name): Hello.Response {
        val getSuspend = CoroutineScope(SchedulerAndDispatcher.IO_DISPATCHER).async {
            printThread("async1")
            testRepository.findById()
        }
        val getFlowAsList = CoroutineScope(SchedulerAndDispatcher.IO_DISPATCHER).async {
            printThread("async2")
            testRepository.findAll().toList()
        }
        val result = "hello ${request.value}\n${getFlowAsList.await()}\n${getSuspend.await()}"

        return Hello.Response.newBuilder()
            .setMessage(result)
            .build()
    }

    private fun printThread(str: String) = println("$str threadName: ${Thread.currentThread().name}")

}
