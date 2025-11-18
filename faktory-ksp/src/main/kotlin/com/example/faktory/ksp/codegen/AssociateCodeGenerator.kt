package com.example.faktory.ksp.codegen

import com.example.faktory.ksp.metadata.ForeignKeyConstraint
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec

object AssociateCodeGenerator {
    fun generateAssociateExtension(fk: ForeignKeyConstraint): FunSpec {
        // Remove trailing 's' to get singular form: "users" → "user"
        val functionName = fk.referencedTable.removeSuffix("s")

        return FunSpec.builder(functionName)
            .receiver(ClassName("com.example.faktory.core", "AssociationContext"))
            .addParameter(
                ParameterSpec.builder(
                    "block",
                    LambdaTypeName.get(
                        returnType = ClassName("", fk.referencedRecordType),
                    ),
                ).build(),
            )
            .addStatement("associateWithPersist(%S, block)", fk.fieldName)
            .build()
    }
}
