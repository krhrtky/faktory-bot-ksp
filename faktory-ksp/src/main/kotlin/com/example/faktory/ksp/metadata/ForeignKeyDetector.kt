package com.example.faktory.ksp.metadata

import org.jooq.Table

data class ForeignKeyConstraint(
    val fieldName: String,
    val referencedTable: String,
    val referencedRecordType: String,
)

object ForeignKeyDetector {
    fun detect(table: Table<*>): List<ForeignKeyConstraint> {
        return table.references
            .flatMap { foreignKey ->
                foreignKey.fields.map { field ->
                    ForeignKeyConstraint(
                        fieldName = field.name,
                        referencedTable = foreignKey.key.table.name,
                        referencedRecordType = foreignKey.key.table.name.toPascalCase() + "Record",
                    )
                }
            }
    }

    private fun String.toPascalCase(): String =
        split("_").joinToString("") { part ->
            part.replaceFirstChar { it.uppercaseChar() }
        }
}
