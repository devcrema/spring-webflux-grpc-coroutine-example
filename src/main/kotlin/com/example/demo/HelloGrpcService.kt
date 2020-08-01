package com.example.demo

import org.lognet.springboot.grpc.GRpcService
import reactor.core.publisher.Mono

@GRpcService
class HelloGrpcService : ReactorHelloServiceGrpc.HelloServiceImplBase() {
    override fun getHello(request: Mono<Hello.Name>): Mono<Hello.Response> = request.map {
        Hello.Response.newBuilder()
                .setMessage("hello ${it.value}")
                .build()
    }
}
