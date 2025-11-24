package com.example.faktory.ksp.metadata

import org.jooq.Table

data class TableMetadata(
    val tableName: String,
    val requiredFields: List<String> = emptyList(),
    val optionalFields: List<String> = emptyList(),
    val foreignKeys: List<ForeignKeyConstraint> = emptyList(),
)

object JooqMetadataExtractor {
    fun extract(table: Table<*>): TableMetadata {
        val allFields = table.fields().filter { it.name != "id" }
        val requiredFields =
            allFields
                .filter { field -> !field.dataType.nullable() }
                .map { it.name }
        val optionalFields =
            allFields
                .filter { field -> field.dataType.nullable() }
                .map { it.name }
        val foreignKeys = ForeignKeyDetector.detect(table)

        return TableMetadata(
            tableName = table.name,
            requiredFields = requiredFields,
            optionalFields = optionalFields,
            foreignKeys = foreignKeys,
        )
    }
}
