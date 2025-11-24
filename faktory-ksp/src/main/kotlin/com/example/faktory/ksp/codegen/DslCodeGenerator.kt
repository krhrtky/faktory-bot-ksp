package com.example.faktory.ksp.codegen

import com.example.faktory.ksp.PackageInfo
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
        packageInfo: PackageInfo,
    ): String {
        val baseName = recordClassName.removeSuffix("Record")
        val builderClassName = "${baseName}DslBuilder"
        val factoryFunctionName = metadata.tableName.removeSuffix("s")

        val file =
            FileSpec.builder(packageInfo.factoryPackage, "Generated")
                .addType(generateDslBuilder(builderClassName, recordClassName, metadata, packageInfo))
                .addFunction(
                    generateFactoryFunction(
                        factoryFunctionName,
                        builderClassName,
                        recordClassName,
                        metadata,
                        packageInfo,
                    ),
                )
                .build()

        return file.toString()
    }

    private fun generateDslBuilder(
        builderClassName: String,
        recordClassName: String,
        metadata: TableMetadata,
        packageInfo: PackageInfo,
    ): TypeSpec {
        val factoryDslAnnotation =
            AnnotationSpec.builder(
                ClassName("com.example.faktory.core", "FactoryDsl"),
            ).build()

        val foreignKeyMap = metadata.foreignKeys.associate { it.fieldName to it.referencedTable }

        val constructorParams =
            metadata.requiredFields.map { fieldName ->
                val foreignKey = foreignKeyMap[fieldName]
                if (foreignKey != null) {
                    val paramName = fieldName.removeSuffix("_id").toCamelCase()
                    val refRecordClassName = "${foreignKey.toPascalCase()}Record"
                    ParameterSpec.builder(
                        paramName,
                        ClassName(packageInfo.recordPackage, refRecordClassName),
                    ).build()
                } else {
                    ParameterSpec.builder(
                        fieldName.toCamelCase(),
                        String::class,
                    ).build()
                }
            }

        val requiredProperties =
            metadata.requiredFields.map { fieldName ->
                val foreignKey = foreignKeyMap[fieldName]
                if (foreignKey != null) {
                    val paramName = fieldName.removeSuffix("_id").toCamelCase()
                    val refRecordClassName = "${foreignKey.toPascalCase()}Record"
                    PropertySpec.builder(
                        paramName,
                        ClassName(packageInfo.recordPackage, refRecordClassName),
                        KModifier.PUBLIC,
                    ).mutable(true)
                        .initializer(paramName)
                        .build()
                } else {
                    PropertySpec.builder(
                        fieldName.toCamelCase(),
                        String::class,
                        KModifier.PUBLIC,
                    ).mutable(true)
                        .initializer(fieldName.toCamelCase())
                        .build()
                }
            }

        val optionalProperties =
            metadata.optionalFields.map { fieldName ->
                val fieldType = getFieldType(fieldName)
                PropertySpec.builder(
                    fieldName.toCamelCase(),
                    fieldType.copy(nullable = true),
                    KModifier.PUBLIC,
                ).mutable(true)
                    .initializer("null")
                    .build()
            }

        val properties = requiredProperties + optionalProperties

        val buildFunction = generateBuildFunction(recordClassName, metadata, foreignKeyMap, packageInfo)

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

    private fun generateBuildFunction(
        recordClassName: String,
        metadata: TableMetadata,
        foreignKeyMap: Map<String, String>,
        packageInfo: PackageInfo,
    ): FunSpec {
        val statements = mutableListOf<String>()

        metadata.requiredFields.forEach { fieldName ->
            val foreignKey = foreignKeyMap[fieldName]
            if (foreignKey != null) {
                val paramName = fieldName.removeSuffix("_id").toCamelCase()
                statements.add("this.${fieldName.toCamelCase()} = this@${recordClassName.removeSuffix("Record")}DslBuilder.$paramName.id")
            } else {
                statements.add("this.${fieldName.toCamelCase()} = this@${recordClassName.removeSuffix("Record")}DslBuilder.${fieldName.toCamelCase()}")
            }
        }

        metadata.optionalFields.forEach { fieldName ->
            statements.add("this.${fieldName.toCamelCase()} = this@${recordClassName.removeSuffix("Record")}DslBuilder.${fieldName.toCamelCase()}")
        }

        return FunSpec.builder("build")
            .addModifiers(KModifier.INTERNAL)
            .returns(ClassName(packageInfo.recordPackage, recordClassName))
            .addCode(
                """
                return %T().apply {
                    ${statements.joinToString("\n                    ")}
                }
                """.trimIndent(),
                ClassName(packageInfo.recordPackage, recordClassName),
            )
            .build()
    }

    private fun generateFactoryFunction(
        functionName: String,
        builderClassName: String,
        recordClassName: String,
        metadata: TableMetadata,
        packageInfo: PackageInfo,
    ): FunSpec {
        val foreignKeyMap = metadata.foreignKeys.associate { it.fieldName to it.referencedTable }

        val requiredParams =
            metadata.requiredFields.map { fieldName ->
                val foreignKey = foreignKeyMap[fieldName]
                if (foreignKey != null) {
                    val paramName = fieldName.removeSuffix("_id").toCamelCase()
                    val refRecordClassName = "${foreignKey.toPascalCase()}Record"
                    ParameterSpec.builder(
                        paramName,
                        ClassName(packageInfo.recordPackage, refRecordClassName),
                    ).build()
                } else {
                    ParameterSpec.builder(
                        fieldName.toCamelCase(),
                        String::class,
                    ).build()
                }
            }

        val blockParam =
            ParameterSpec.builder(
                "block",
                LambdaTypeName.get(
                    receiver = ClassName(packageInfo.factoryPackage, builderClassName),
                    returnType = UNIT,
                ),
            ).defaultValue("{}")
                .build()

        val paramNames =
            metadata.requiredFields.map { fieldName ->
                val foreignKey = foreignKeyMap[fieldName]
                if (foreignKey != null) {
                    fieldName.removeSuffix("_id").toCamelCase()
                } else {
                    fieldName.toCamelCase()
                }
            }

        return FunSpec.builder(functionName)
            .addParameters(requiredParams)
            .addParameter(blockParam)
            .returns(ClassName(packageInfo.recordPackage, recordClassName))
            .addStatement(
                "return %T(${paramNames.joinToString(", ")}).apply(block).build()",
                ClassName(packageInfo.factoryPackage, builderClassName),
            )
            .build()
    }

    private fun String.toCamelCase(): String =
        split("_").mapIndexed { index, part ->
            if (index == 0) part else part.replaceFirstChar { it.uppercaseChar() }
        }.joinToString("")

    private fun String.toPascalCase(): String =
        split("_").joinToString("") { it.replaceFirstChar { c -> c.uppercaseChar() } }

    private fun getFieldType(fieldName: String): ClassName {
        return when (fieldName) {
            "age" -> ClassName("kotlin", "Int")
            "published" -> ClassName("kotlin", "Boolean")
            "created_at", "updated_at" -> ClassName("java.time", "LocalDateTime")
            else -> ClassName("kotlin", "String")
        }
    }
}
