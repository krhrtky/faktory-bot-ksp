package com.example.faktory.ksp.codegen

import com.example.faktory.ksp.metadata.TableMetadata
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.asClassName

object DslCodeGenerator {
    fun generate(
        recordClassName: String,
        metadata: TableMetadata,
    ): String {
        val baseName = recordClassName.removeSuffix("Record")
        val builderClassName = "${baseName}DslBuilder"
        val factoryFunctionName = metadata.tableName.removeSuffix("s")

        val file = FileSpec.builder("", "Generated")
            .addType(generateDslBuilder(builderClassName, recordClassName, metadata))
            .addFunction(
                generateFactoryFunction(
                    factoryFunctionName,
                    builderClassName,
                    recordClassName,
                    metadata,
                ),
            )
            .build()

        return file.toString()
    }

    private fun generateDslBuilder(
        builderClassName: String,
        recordClassName: String,
        metadata: TableMetadata,
    ): TypeSpec {
        val factoryDslAnnotation = AnnotationSpec.builder(
            ClassName("com.example.faktory.core", "FactoryDsl"),
        ).build()

        val constructorParams = metadata.requiredFields.map { fieldName ->
            ParameterSpec.builder(
                fieldName.toCamelCase(),
                String::class,
            ).build()
        }

        val requiredProperties = metadata.requiredFields.map { fieldName ->
            PropertySpec.builder(
                fieldName.toCamelCase(),
                String::class,
                KModifier.PUBLIC,
            ).mutable(true)
                .initializer(fieldName.toCamelCase())
                .build()
        }

        val optionalProperties = metadata.optionalFields.map { fieldName ->
            PropertySpec.builder(
                fieldName.toCamelCase(),
                String::class.asClassName().copy(nullable = true),
                KModifier.PUBLIC,
            ).mutable(true)
                .initializer("null")
                .build()
        }

        val properties = requiredProperties + optionalProperties

        val buildFunction = FunSpec.builder("build")
            .addModifiers(KModifier.INTERNAL)
            .returns(ClassName("", recordClassName))
            .addStatement("return %T()", ClassName("", recordClassName))
            .build()

        return TypeSpec.classBuilder(builderClassName)
            .addAnnotation(factoryDslAnnotation)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameters(constructorParams)
                    .build(),
            )
            .addProperties(properties)
            .addFunction(buildFunction)
            .build()
    }

    private fun generateFactoryFunction(
        functionName: String,
        builderClassName: String,
        recordClassName: String,
        metadata: TableMetadata,
    ): FunSpec {
        val requiredParams = metadata.requiredFields.map { fieldName ->
            ParameterSpec.builder(
                fieldName.toCamelCase(),
                String::class,
            ).build()
        }

        val blockParam = ParameterSpec.builder(
            "block",
            LambdaTypeName.get(
                receiver = ClassName("", builderClassName),
                returnType = UNIT,
            ),
        ).defaultValue("{}")
            .build()

        return FunSpec.builder(functionName)
            .addParameters(requiredParams)
            .addParameter(blockParam)
            .returns(ClassName("", recordClassName))
            .addStatement(
                "return %T(${metadata.requiredFields.joinToString(", ") { it.toCamelCase() }}).apply(block).build()",
                ClassName("", builderClassName),
            )
            .build()
    }

    private fun String.toCamelCase(): String =
        split("_").mapIndexed { index, part ->
            if (index == 0) part else part.replaceFirstChar { it.uppercaseChar() }
        }.joinToString("")
}
