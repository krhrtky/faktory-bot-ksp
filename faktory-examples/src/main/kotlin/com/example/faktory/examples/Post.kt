package com.example.faktory.examples

data class Post(
    val id: Int? = null,
    val userId: Int,
    val title: String,
    val content: String,
    val published: Boolean = false,
)
