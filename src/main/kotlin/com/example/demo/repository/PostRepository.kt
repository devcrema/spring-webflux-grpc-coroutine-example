package com.example.demo.repository

import com.example.demo.entity.Post
import org.springframework.data.repository.reactive.ReactiveSortingRepository

interface PostRepository : ReactiveSortingRepository<Post, Long>
