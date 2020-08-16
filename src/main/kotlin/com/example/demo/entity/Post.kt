package com.example.demo.entity

import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToMany

@Entity
class Post(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private val id: Long = 0L,
        val title: String,
        @Column(columnDefinition = "TEXT")
        val content: String,
        var popularity: Long, // increase by add comments
        @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, mappedBy = "postId")
        val comments: MutableList<Comment>
) {
    fun addComment(content: String) =
            Comment(content = content, postId = this.id)
                    .also { comments.add(it) }

    fun increasePopularity() = this.popularity++
}
