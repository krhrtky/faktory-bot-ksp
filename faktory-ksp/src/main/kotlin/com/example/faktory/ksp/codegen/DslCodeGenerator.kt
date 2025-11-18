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

        val fileBuilder = FileSpec.builder("", "Generated")
            .addType(generateDslBuilder(builderClassName, recordClassName, metadata))
            .addFunction(
                generateFactoryFunction(
                    factoryFunctionName,
                    builderClassName,
                    recordClassName,
                    metadata,
                ),
            )

        // 外部キーがある場合、associate extension関数を生成
        metadata.foreignKeys.forEach { fk ->
            val extension = AssociateCodeGenerator.generateAssociateExtension(fk)
            fileBuilder.addFunction(extension)
        }

        val file = fileBuilder.build()

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

        // 外部キーフィールドを除外
        val fkFieldNames = metadata.foreignKeys.map { it.fieldName }
        val nonFkRequiredFields = metadata.requiredFields.filterNot { it in fkFieldNames }

        val constructorParams = nonFkRequiredFields.map { fieldName ->
            ParameterSpec.builder(
                fieldName.toCamelCase(),
                String::class,
            ).build()
        }

        val requiredProperties = nonFkRequiredFields.map { fieldName ->
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

        // 外部キーフィールドをオプショナルプロパティとして追加
        val fkProperties = metadata.foreignKeys.map { fk ->
            PropertySpec.builder(
                fk.fieldName.toCamelCase(),
                Int::class.asClassName().copy(nullable = true),
                KModifier.PUBLIC,
            ).mutable(true)
                .initializer("null")
                .build()
        }

        // 外部キーがある場合、AssociationContextプロパティを追加
        val additionalProperties = if (metadata.foreignKeys.isNotEmpty()) {
            listOf(
                PropertySpec.builder(
                    "associationContext",
                    ClassName("com.example.faktory.core", "AssociationContext"),
                ).initializer("%T()", ClassName("com.example.faktory.core", "AssociationContext"))
                    .build(),
            )
        } else {
            emptyList()
        }

        val properties = requiredProperties + optionalProperties + fkProperties + additionalProperties

        val buildFunction = FunSpec.builder("build")
            .addModifiers(KModifier.INTERNAL)
            .returns(ClassName("", recordClassName))
            .addStatement("return %T()", ClassName("", recordClassName))
            .build()

        // 外部キーがある場合のみassociateメソッドを生成
        val functions = mutableListOf(buildFunction)
        if (metadata.foreignKeys.isNotEmpty()) {
            val associateMethod = FunSpec.builder("associate")
                .addParameter(
                    "block",
                    LambdaTypeName.get(
                        receiver = ClassName("com.example.faktory.core", "AssociationContext"),
                        returnType = UNIT,
                    ),
                )
                .addStatement("associationContext.block()")
                .build()
            functions.add(associateMethod)
        }

        return TypeSpec.classBuilder(builderClassName)
            .addAnnotation(factoryDslAnnotation)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameters(constructorParams)
                    .build(),
            )
            .addProperties(properties)
            .addFunctions(functions)
            .build()
    }

    private fun generateFactoryFunction(
        functionName: String,
        builderClassName: String,
        recordClassName: String,
        metadata: TableMetadata,
    ): FunSpec {
        // 外部キーフィールドを除外
        val fkFieldNames = metadata.foreignKeys.map { it.fieldName }
        val nonFkRequiredFields = metadata.requiredFields.filterNot { it in fkFieldNames }

        val requiredParams = nonFkRequiredFields.map { fieldName ->
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
                "return %T(${nonFkRequiredFields.joinToString(", ") { it.toCamelCase() }}).apply(block).build()",
                ClassName("", builderClassName),
            )
            .build()
    }

    private fun String.toCamelCase(): String =
        split("_").mapIndexed { index, part ->
            if (index == 0) part else part.replaceFirstChar { it.uppercaseChar() }
        }.joinToString("")
}
