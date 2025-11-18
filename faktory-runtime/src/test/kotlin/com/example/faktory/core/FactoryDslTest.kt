package com.example.faktory.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.reflect.full.findAnnotation

class FactoryDslTest {
    @Test
    fun `@FactoryDsl annotation exists and is applicable to classes`() {
        val target = FactoryDsl::class.findAnnotation<Target>()

        assertThat(target).isNotNull
        assertThat(target?.allowedTargets)
            .contains(AnnotationTarget.CLASS)
    }

    @Test
    fun `@FactoryDsl annotation can be applied to a class`() {
        @FactoryDsl
        class TestDslBuilder {
            var name: String = ""
        }

        val factoryDsl = TestDslBuilder::class.findAnnotation<FactoryDsl>()
        assertThat(factoryDsl).isNotNull
    }
}
