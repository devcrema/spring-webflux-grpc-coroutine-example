package com.example.demo.service

import com.example.demo.entity.Post
import com.example.demo.repository.PostRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

@Service
class AddCommentService(private val postRepository: PostRepository) {

    fun addComment(postId: Long, commentContent: String): Mono<Post> =
            Mono.justOrEmpty(postRepository.findByIdOrNull(postId))
                    .switchIfEmpty { Mono.error(IllegalArgumentException()) }
                    .map { post ->
                        post.addComment(commentContent)
                        postRepository.save(post)
                    }
                    .map { post ->
                        post.increasePopularity()
                        postRepository.save(post)
                    }
}
