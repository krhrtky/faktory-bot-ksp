package com.example.faktory.examples

data class Comment(
    val id: Int? = null,
    val postId: Int,
    val userId: Int,
    val content: String,
)
