package com.example.faktory.ksp.codegen

import com.example.faktory.ksp.metadata.TableMetadata
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec

object PhantomTypeGenerator {
    fun generate(
        recordClassName: String,
        metadata: TableMetadata,
    ): String {
        val baseName = recordClassName.removeSuffix("Record")
        val stateName = "${baseName}FieldState"

        val sealedInterface = TypeSpec.interfaceBuilder(stateName).apply {
            addModifiers(com.squareup.kotlinpoet.KModifier.SEALED)

            metadata.requiredFields.forEach { fieldName ->
                val objectName = "With${fieldName.toCamelCase()}"
                val stateObject = TypeSpec.objectBuilder(objectName).apply {
                    addSuperinterface(ClassName("", stateName))
                }.build()
                addType(stateObject)
            }

            val completeObject = TypeSpec.objectBuilder("Complete").apply {
                addSuperinterface(ClassName("", stateName))
            }.build()
            addType(completeObject)
        }.build()

        return sealedInterface.toString()
    }

    private fun String.toCamelCase(): String =
        split("_").joinToString("") { it.replaceFirstChar { c -> c.uppercaseChar() } }
}
