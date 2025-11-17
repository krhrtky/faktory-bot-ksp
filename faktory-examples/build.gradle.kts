plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    id("nu.studer.jooq") version "8.2"
}

dependencies {
    implementation(project(":faktory-runtime"))
    ksp(project(":faktory-ksp"))

    implementation("org.jooq:jooq:3.18.7")
    implementation("org.jooq:jooq-kotlin:3.18.7")
    implementation("com.zaxxer:HikariCP:5.1.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:postgresql:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    testImplementation("org.postgresql:postgresql:42.7.1")

    jooqGenerator("org.jooq:jooq-meta-extensions:3.18.7")
}

jooq {
    version.set("3.18.7")
    configurations {
        create("main") {
            jooqConfiguration.apply {
                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"
                    database.apply {
                        name = "org.jooq.meta.extensions.ddl.DDLDatabase"
                        properties.add(
                            org.jooq.meta.jaxb.Property().apply {
                                key = "scripts"
                                value = "src/test/resources/schema.sql"
                            },
                        )
                        properties.add(
                            org.jooq.meta.jaxb.Property().apply {
                                key = "defaultNameCase"
                                value = "lower"
                            },
                        )
                    }
                    target.apply {
                        packageName = "com.example.faktory.examples.jooq"
                        directory = "build/generated-jooq"
                    }
                    generate.apply {
                        isDeprecated = false
                        isRecords = true
                        isImmutablePojos = false
                        isFluentSetters = true
                    }
                }
            }
        }
    }
}

sourceSets {
    main {
        java {
            srcDir("build/generated-jooq")
        }
    }
}

tasks.test {
    environment("DOCKER_HOST", "unix:///Users/takuya.kurihara/.colima/default/docker.sock")
    environment("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE", "/Users/takuya.kurihara/.colima/default/docker.sock")
    environment("TESTCONTAINERS_RYUK_DISABLED", "true")
}
