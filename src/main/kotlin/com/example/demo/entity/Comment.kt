package com.example.demo.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
class Comment(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private val id: Long = 0L,
        val content: String,
        @Column(name = "post_id")
        val postId: Long
)
