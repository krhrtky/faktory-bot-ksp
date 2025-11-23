plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.google.devtools.ksp:symbol-processing-api:1.9.21-1.0.15")
    implementation("com.squareup:kotlinpoet:1.15.3")
    implementation("com.squareup:kotlinpoet-ksp:1.15.3")
    implementation("org.jooq:jooq:3.18.7")

    testImplementation(project(":faktory-examples"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.5.0")
    testImplementation("org.jooq:jooq-codegen:3.18.7")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "faktory-ksp"

            pom {
                name.set("Faktory KSP")
                description.set("KSP processor for faktory-bot-ksp - compile-time factory generation for jOOQ")
                url.set("https://github.com/krhrtky/faktory-bot-ksp")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("krhrtky")
                        name.set("krhrtky")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/krhrtky/faktory-bot-ksp.git")
                    developerConnection.set("scm:git:ssh://github.com/krhrtky/faktory-bot-ksp.git")
                    url.set("https://github.com/krhrtky/faktory-bot-ksp")
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/krhrtky/faktory-bot-ksp")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
