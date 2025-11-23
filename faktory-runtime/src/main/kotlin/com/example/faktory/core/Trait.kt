package com.example.faktory.core

interface Trait<B : FactoryBuilder<*>> {
    fun apply(builder: B): B
}
