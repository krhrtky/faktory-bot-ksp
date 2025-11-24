package com.example.faktory.core

interface Trait<B : FactoryBuilder<*>> {
    fun apply(builder: B): B
}

interface DslTrait<B> {
    operator fun invoke(builder: B)
}

fun <B> B.trait(trait: DslTrait<B>) {
    trait(this)
}

fun <B> B.trait(vararg traits: DslTrait<B>) {
    traits.forEach { it(this) }
}
