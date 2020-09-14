package com.example.demo.entity

import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
data class User(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private val id: Long = 0L,
        val email: String,
        var enabled: Boolean,
        val lastAccessedAt: LocalDateTime = LocalDateTime.now(UTC)
)
