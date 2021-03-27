package com.example.demo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
class TestRepository(
    private val testReactiveRepository: TestReactiveRepository,
    private val testJpaRepository: TestJpaRepository
){

    suspend fun findById(): String? = testReactiveRepository.findById().awaitFirstOrNull()
    suspend fun findAll(): Flow<String> = testReactiveRepository.findAll().asFlow()

    // unlike Mono, you can just call it with null
    suspend fun findByIdWithJpa(): String? = testJpaRepository.findByIdOrNull()
    suspend fun findAllWithJpa(): List<String> = testJpaRepository.findAll()
}

@Repository
class TestReactiveRepository{
    fun findAll(): Flux<String> = Flux.just("this ", " is ", " coroutine", " flow")
    fun findById(): Mono<String> = Mono.just("hello world!!!")
}

@Repository
class TestJpaRepository {
    fun findAll(): List<String> = listOf("this ", " is ", " coroutine ", " flow ")
    fun findByIdOrNull(): String? = "hell world!!!"
}
