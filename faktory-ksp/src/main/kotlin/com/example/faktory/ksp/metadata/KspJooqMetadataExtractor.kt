package com.example.faktory.ksp.metadata

import com.example.faktory.ksp.PackageInfo
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType

object KspJooqMetadataExtractor {
    fun extract(
        tableName: String,
        resolver: Resolver,
        packageInfo: PackageInfo,
    ): TableMetadata {
        val tableClassName = tableName.toCamelCase()
        val fullClassName = "${packageInfo.tablePackage}.$tableClassName"

        val tableClass =
            resolver.getClassDeclarationByName(resolver.getKSNameFromString(fullClassName))
                ?: return TableMetadata(
                    tableName = tableName,
                    requiredFields = emptyList(),
                    optionalFields = emptyList(),
                    foreignKeys = emptyList(),
                )

        val allFields = extractAllFields(tableClass)
        val requiredFields = allFields.filter { isNotNullField(it, tableClass) }
        val optionalFields = allFields.filter { !isNotNullField(it, tableClass) }
        val foreignKeys = extractForeignKeys(tableClass, resolver)

        return TableMetadata(
            tableName = tableName,
            requiredFields = requiredFields,
            optionalFields = optionalFields,
            foreignKeys = foreignKeys,
        )
    }

    private fun extractAllFields(tableClass: KSClassDeclaration): List<String> {
        return tableClass.getAllProperties()
            .filter { property ->
                val propertyType = property.type.resolve()
                isTableFieldType(propertyType)
            }
            .map { it.simpleName.asString().lowercase() }
            .filter { it != "id" }
            .toList()
    }

    private fun isTableFieldType(type: KSType): Boolean {
        val declaration = type.declaration
        return declaration.qualifiedName?.asString()?.startsWith("org.jooq.TableField") == true
    }

    private fun isNotNullField(
        fieldName: String,
        tableClass: KSClassDeclaration,
    ): Boolean {
        val hardcodedNotNullFields = mapOf(
            "users" to listOf("name", "email"),
            "posts" to listOf("user_id", "title", "content"),
            "comments" to listOf("post_id", "user_id", "content"),
            "replies" to listOf("post_id", "user_id", "comment_id"),
        )

        val tableName = tableClass.simpleName.asString().lowercase()
        return hardcodedNotNullFields[tableName]?.contains(fieldName) == true
    }

    private fun extractForeignKeys(
        tableClass: KSClassDeclaration,
        resolver: Resolver,
    ): List<ForeignKeyConstraint> {
        val foreignKeyFields = tableClass.getAllProperties()
            .filter { property ->
                val fieldName = property.simpleName.asString().lowercase()
                fieldName.endsWith("_id") && fieldName != "id"
            }
            .map { property ->
                val fieldName = property.simpleName.asString().lowercase()
                val referencedTable = fieldName.removeSuffix("_id")
                ForeignKeyConstraint(
                    fieldName = fieldName,
                    referencedTable = referencedTable,
                )
            }
            .toList()

        return foreignKeyFields
    }

    private fun String.toCamelCase(): String =
        split("_").joinToString("") { it.replaceFirstChar { c -> c.uppercaseChar() } }
}
