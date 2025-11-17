package com.example.faktory.ksp.metadata

import org.jooq.Table

data class TableMetadata(
    val tableName: String,
    val requiredFields: List<String> = emptyList(),
)

object JooqMetadataExtractor {
    fun extract(table: Table<*>): TableMetadata {
        val requiredFields = table.fields()
            .filter { field -> !field.dataType.nullable() && field.name != "id" }
            .map { it.name }

        return TableMetadata(
            tableName = table.name,
            requiredFields = requiredFields,
        )
    }
}
