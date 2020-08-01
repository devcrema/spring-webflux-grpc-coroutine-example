package com.example.demo.service

import com.example.demo.entity.Post
import com.example.demo.repository.PostRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

@Service
class AddCommentService(private val postRepository: PostRepository) {

    fun addComment(postId: Long, commentContent: String): Mono<Post> =
            postRepository.findById(postId)
                    .switchIfEmpty { Mono.error(IllegalArgumentException()) }
                    .flatMap { post ->
                        post.addComment(commentContent)
                        postRepository.save(post)
                    }
                    .flatMap { post ->
                        post.increasePopularity()
                        postRepository.save(post)
                    }
}
