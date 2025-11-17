package com.example.faktory.ksp.codegen

import com.example.faktory.ksp.metadata.TableMetadata
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName

object BuilderCodeGenerator {
    fun generate(
        recordClassName: String,
        metadata: TableMetadata,
    ): String {
        val baseName = recordClassName.removeSuffix("Record")
        val builderName = "${baseName}FactoryBuilder"
        val stateName = "${baseName}FieldState"
        val stateClassName = ClassName("", stateName)

        val typeParameter = TypeVariableName("S", stateClassName)

        val builder = TypeSpec.interfaceBuilder(builderName).apply {
            addTypeVariable(typeParameter)

            metadata.requiredFields.forEach { fieldName ->
                val methodName = "with${fieldName.toCamelCase()}"
                val stateObjectName = "With${fieldName.toCamelCase()}"
                val returnType = ClassName("", builderName)
                    .parameterizedBy(ClassName("", stateName, stateObjectName))

                addFunction(
                    FunSpec.builder(methodName)
                        .addParameter("value", String::class)
                        .returns(returnType)
                        .build(),
                )
            }

            val recordClass = ClassName("", recordClassName)
            val completeState = ClassName("", stateName, "Complete")

            addFunction(
                FunSpec.builder("build")
                    .addTypeVariable(TypeVariableName("S", completeState))
                    .returns(recordClass)
                    .build(),
            )
        }.build()

        return builder.toString()
    }

    private fun String.toCamelCase(): String =
        split("_").joinToString("") { it.replaceFirstChar { c -> c.uppercaseChar() } }
}
