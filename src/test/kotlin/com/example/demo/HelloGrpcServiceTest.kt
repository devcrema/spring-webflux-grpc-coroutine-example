package com.example.demo

import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Profile
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@Profile("test")
@SpringBootTest
internal class HelloGrpcServiceTest {

    @Value("\${grpc.port}")
    val port: Int = 0

    @Autowired
    lateinit var helloGrpcService: HelloGrpcService

    @Test
    fun getHelloWithGrpcClient() {
        //given
        val channel = ManagedChannelBuilder.forAddress("0.0.0.0", port)
                .usePlaintext() //for test, it disable TLS
                .build()
        val nameWithReactorStub = ReactorHelloServiceGrpc.newReactorStub(channel) //기본 stub을 wrapping한 reactor용 stub (mono or flux) salesforce/reactive-grpc 의존성.
        val nameWithBlockingStub = HelloServiceGrpc.newBlockingStub(channel) //blocking stub
        val nameWithAsyncStub = HelloServiceGrpc.newStub(channel) //기본이 async
        val request = Hello.Name.newBuilder()
                .setValue("world!!!!")
                .build()
        val expected = "hello ${request.value}"

        //when, then
        nameWithReactorStub.getHello(request).block().also { assertEquals(expected, it!!.message) }
        nameWithBlockingStub.getHello(request).also { assertEquals(expected, it!!.message) }
        nameWithAsyncStub.getHello(request, object : StreamObserver<Hello.Response> {
            override fun onNext(value: Hello.Response?) {
                assertEquals(expected, value!!.message)
            }

            override fun onError(t: Throwable?) {
                fail { "on Error: ${t?.message}" }
            }

            override fun onCompleted() {
                println("onCompleted")
            }

        })
        channel.shutdown() //forced shutdown : shutdownNow()
    }

    @Test
    fun getHelloWithWebflux() {
        //given
        val request = Mono.just(Hello.Name.newBuilder()
                .setValue("world!!!!")
                .build())
        val expected = "hello ${request.block()!!.value}"
        //when
        val response = helloGrpcService.getHello(request)
        //then
        StepVerifier.create(response.map { it.message })
                .expectNext(expected)
                .verifyComplete()
    }
}