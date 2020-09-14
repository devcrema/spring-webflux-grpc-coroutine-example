package com.example.demo.repository

import com.example.demo.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface UserRepository: JpaRepository<User, Long> {
    fun findAllByLastAccessedAtBefore(criteriaTime: LocalDateTime, pageable: Pageable): Page<User>
}