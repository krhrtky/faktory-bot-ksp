package com.example.faktory.ksp.codegen

import com.example.faktory.ksp.metadata.ForeignKeyConstraint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AssociateCodeGeneratorTest {
    @Test
    fun `generateAssociateExtension() creates extension function for foreign key`() {
        val fk = ForeignKeyConstraint(
            fieldName = "user_id",
            referencedTable = "users",
            referencedRecordType = "UsersRecord",
        )

        val extension = AssociateCodeGenerator.generateAssociateExtension(fk)
        val code = extension.toString()

        // Extension function name is based on referenced table (singular)
        assertThat(code).contains("fun AssociationContext.user(")
        // Parameter is a lambda returning the referenced record type
        assertThat(code).contains("block: () -> UsersRecord")
        // Calls associateWithPersist with field name and block
        assertThat(code).contains("associateWithPersist(\"user_id\", block)")
    }

    @Test
    fun `generateAssociateExtension() handles snake_case table names`() {
        val fk = ForeignKeyConstraint(
            fieldName = "blog_post_id",
            referencedTable = "blog_posts",
            referencedRecordType = "BlogPostsRecord",
        )

        val extension = AssociateCodeGenerator.generateAssociateExtension(fk)
        val code = extension.toString()

        // Function name should be singular and camelCase
        assertThat(code).contains("fun AssociationContext.blogPost(")
        assertThat(code).contains("block: () -> BlogPostsRecord")
        assertThat(code).contains("associateWithPersist(\"blog_post_id\", block)")
    }
}
