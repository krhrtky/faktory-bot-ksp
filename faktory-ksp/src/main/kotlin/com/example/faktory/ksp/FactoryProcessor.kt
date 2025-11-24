package com.example.faktory.ksp

import com.example.faktory.ksp.codegen.DslCodeGenerator
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
        val factoryPackage = classDeclaration.packageName.asString()

        val baseName = tableName.split("_").joinToString("") { it.replaceFirstChar { c -> c.uppercaseChar() } }
        val recordClassName = "${baseName}Record"

        val recordClass = findRecordClass(resolver, recordClassName)
            ?: throw IllegalStateException("Record class not found: $recordClassName")

        val recordPackage = recordClass.packageName.asString()
        val tablePackage = recordPackage.replace(".records", "")

        val packageInfo = PackageInfo(
            factoryPackage = factoryPackage,
            tablePackage = tablePackage,
            recordPackage = recordPackage,
        )

        val metadata = KspJooqMetadataExtractor.extract(tableName, resolver, packageInfo)

        val fileName = "${baseName}Dsl"
        val generatedCode = DslCodeGenerator.generate(recordClassName, metadata, packageInfo)

        codeGenerator.createNewFile(
            dependencies = Dependencies(false, containingFile),
            packageName = factoryPackage,
            fileName = fileName,
        ).bufferedWriter().use { it.write(generatedCode) }
    }

    private fun findRecordClass(
        resolver: Resolver,
        recordClassName: String,
    ): KSClassDeclaration? {
        return resolver.getAllFiles()
            .flatMap { it.declarations }
            .filterIsInstance<KSClassDeclaration>()
            .firstOrNull { it.simpleName.asString() == recordClassName }
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
