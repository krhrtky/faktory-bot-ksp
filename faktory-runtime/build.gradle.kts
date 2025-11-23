plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.jooq:jooq:3.18.7")
    implementation("org.jooq:jooq-kotlin:3.18.7")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("io.mockk:mockk:1.13.8")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "faktory-runtime"

            pom {
                name.set("Faktory Runtime")
                description.set("Runtime library for faktory-bot-ksp - type-safe factory DSL for jOOQ")
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
