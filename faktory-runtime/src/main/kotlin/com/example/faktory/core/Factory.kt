package com.example.faktory.core

abstract class Factory<T : Any, B : FactoryBuilder<T>> {
    abstract fun builder(): B

    fun build(): T = builder().build()

    fun buildList(count: Int): List<T> =
        (1..count).map { build() }
}

interface FactoryBuilder<T : Any> {
    fun build(): T
}
