package com.example.demo.presentation

import com.example.demo.PostServiceOuterClass.AddCommentRequest
import com.example.demo.PostServiceOuterClass.AddCommentResponse
import com.example.demo.ReactorPostServiceGrpc
import com.example.demo.service.AddCommentService
import org.lognet.springboot.grpc.GRpcService
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@GRpcService
class PostGrpcService(private val addCommentService: AddCommentService)
    : ReactorPostServiceGrpc.PostServiceImplBase() {

    override fun addComment(request: Mono<AddCommentRequest>): Mono<AddCommentResponse> =
            request.map { addCommentService.addComment(it.postId, it.content) }
                    .subscribeOn(Schedulers.elastic())
                    .map { AddCommentResponse.newBuilder().build() }
}
