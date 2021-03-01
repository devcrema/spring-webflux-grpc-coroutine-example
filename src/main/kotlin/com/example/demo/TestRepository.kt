package com.example.demo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
class TestRepository(private val testReactiveRepository: TestReactiveRepository){

    suspend fun findById(): String? = testReactiveRepository.findById().awaitFirstOrNull()
    suspend fun findAll(): Flow<String> = testReactiveRepository.findAll().asFlow()

}

@Repository
class TestReactiveRepository{

    fun findAll(): Flux<String> = Flux.just("this ", " is ", " coroutine", " flow")
    fun findById(): Mono<String> = Mono.just("hello world!!!")
}