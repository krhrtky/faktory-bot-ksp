plugins {
    kotlin("jvm") version "1.9.21" apply false
    id("com.google.devtools.ksp") version "1.9.21-1.0.15" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.0.3" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.4" apply false
}

allprojects {
    group = "com.example.faktory"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(17)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.0.1")
        android.set(false)
        outputToConsole.set(true)
        ignoreFailures.set(false)
        filter {
            exclude { it.file.absolutePath.contains("generated-jooq") }
            exclude { it.file.absolutePath.contains("build/") }
        }
    }

    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.file("detekt.yml"))
    }
}
