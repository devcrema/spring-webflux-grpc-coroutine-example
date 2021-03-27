package com.example.demo

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.lognet.springboot.grpc.context.LocalRunningGrpcPort
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Profile
import org.springframework.test.context.TestConstructor

@Profile("test")
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
internal class HelloGrpcServiceTest(@LocalRunningGrpcPort val port: Int) {

    @Test
    fun getHelloWithGrpcStub() {
        //given
        val channel: ManagedChannel = ManagedChannelBuilder.forAddress("0.0.0.0", port)
                .usePlaintext() //for test, it disable TLS
                .build()
        val stub: HelloServiceGrpc.HelloServiceBlockingStub = HelloServiceGrpc.newBlockingStub(channel)
        val request = Hello.Name.newBuilder()
                .setValue("world coroutine!!!!")
                .build()
        val expected = "hello"
        //when
        val result = stub.getHello(request)
        // then
        assertTrue(result!!.message.contains(expected))
    }
}