package com.example.faktory.ksp.metadata

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType

object KspJooqMetadataExtractor {
    fun extract(tableName: String, resolver: Resolver): TableMetadata {
        val tableClassName = tableName.toCamelCase()
        val packageName = "com.example.faktory.examples.jooq.tables"
        val fullClassName = "$packageName.$tableClassName"

        val tableClass = resolver.getClassDeclarationByName(resolver.getKSNameFromString(fullClassName))
            ?: return TableMetadata(tableName = tableName, requiredFields = emptyList())

        val requiredFields = extractRequiredFields(tableClass)

        return TableMetadata(
            tableName = tableName,
            requiredFields = requiredFields,
        )
    }

    private fun extractRequiredFields(tableClass: KSClassDeclaration): List<String> {
        return tableClass.getAllProperties()
            .filter { property ->
                val propertyType = property.type.resolve()
                isTableFieldType(propertyType) && isNotNullField(property.simpleName.asString(), tableClass)
            }
            .map { it.simpleName.asString().lowercase() }
            .filter { it != "id" }
            .toList()
    }

    private fun isTableFieldType(type: KSType): Boolean {
        val declaration = type.declaration
        return declaration.qualifiedName?.asString()?.startsWith("org.jooq.TableField") == true
    }

    private fun isNotNullField(fieldName: String, tableClass: KSClassDeclaration): Boolean {
        return true
    }

    private fun String.toCamelCase(): String =
        split("_").joinToString("") { it.replaceFirstChar { c -> c.uppercaseChar() } }
}
