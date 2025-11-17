plugins {
    kotlin("jvm")
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
