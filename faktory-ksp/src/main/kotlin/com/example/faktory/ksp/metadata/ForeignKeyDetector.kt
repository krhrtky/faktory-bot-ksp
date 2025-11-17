package com.example.faktory.ksp.metadata

import org.jooq.Table

data class ForeignKeyConstraint(
    val fieldName: String,
    val referencedTable: String,
)

object ForeignKeyDetector {
    fun detect(table: Table<*>): List<ForeignKeyConstraint> {
        return table.references
            .flatMap { foreignKey ->
                foreignKey.fields.map { field ->
                    ForeignKeyConstraint(
                        fieldName = field.name,
                        referencedTable = foreignKey.key.table.name,
                    )
                }
            }
    }
}
