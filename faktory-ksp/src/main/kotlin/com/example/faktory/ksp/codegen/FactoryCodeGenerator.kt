package com.example.faktory.ksp.codegen

import com.example.faktory.ksp.metadata.ForeignKeyConstraint
import com.example.faktory.ksp.metadata.TableMetadata
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec

object FactoryCodeGenerator {
    fun generate(
        tableName: String,
        recordClassName: String,
        metadata: TableMetadata,
        foreignKeys: List<ForeignKeyConstraint>,
    ): String {
        val builderInterfaceName = "${recordClassName.removeSuffix("Record")}FactoryBuilder"

        val builder = TypeSpec.interfaceBuilder(builderInterfaceName).apply {
            metadata.requiredFields.forEach { fieldName ->
                val methodName = "with${fieldName.toCamelCase()}"
                addFunction(
                    FunSpec.builder(methodName)
                        .addParameter("value", String::class)
                        .returns(com.squareup.kotlinpoet.ClassName("", builderInterfaceName))
                        .build(),
                )
            }
        }.build()

        return builder.toString()
    }

    private fun String.toCamelCase(): String =
        split("_").joinToString("") { it.replaceFirstChar { c -> c.uppercaseChar() } }
}
