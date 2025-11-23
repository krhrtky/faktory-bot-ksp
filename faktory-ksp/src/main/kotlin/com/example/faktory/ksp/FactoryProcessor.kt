package com.example.faktory.ksp

import com.example.faktory.ksp.codegen.FactoryCodeGenerator
import com.example.faktory.ksp.metadata.KspJooqMetadataExtractor
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

class FactoryProcessor(
    private val environment: SymbolProcessorEnvironment,
) : SymbolProcessor {
    private val codeGenerator: CodeGenerator = environment.codeGenerator

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(Factory::class.qualifiedName!!)

        symbols.filterIsInstance<KSClassDeclaration>().forEach { classDeclaration ->
            processFactory(classDeclaration, resolver)
        }

        return emptyList()
    }

    private fun processFactory(
        classDeclaration: KSClassDeclaration,
        resolver: Resolver,
    ) {
        val containingFile = classDeclaration.containingFile ?: return

        val tableName = extractTableName(classDeclaration) ?: return
        val metadata = KspJooqMetadataExtractor.extract(tableName, resolver)

        val baseName = classDeclaration.simpleName.asString().removeSuffix("Factory")
        val builderName = "${baseName}FactoryBuilder"
        val recordClassName = "${baseName}Record"

        val generatedCode =
            FactoryCodeGenerator.generateComplete(
                tableName = metadata.tableName,
                recordClassName = recordClassName,
                metadata = metadata,
                foreignKeys = emptyList(),
            )

        codeGenerator.createNewFile(
            dependencies = Dependencies(false, containingFile),
            packageName = classDeclaration.packageName.asString(),
            fileName = builderName,
        ).bufferedWriter().use { it.write(generatedCode) }
    }

    private fun extractTableName(classDeclaration: KSClassDeclaration): String? {
        val factoryAnnotation =
            classDeclaration.annotations
                .firstOrNull { it.shortName.asString() == "Factory" }
                ?: return null

        return factoryAnnotation.arguments
            .firstOrNull { it.name?.asString() == "tableName" }
            ?.value as? String
    }
}
