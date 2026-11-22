package com.biojaveriana.api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest
class PostgresSchemaIntegrityTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18")
                    .withDatabaseName("biojaveriana_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void rnF21UniqueConstraintsRejectDuplicateUserAndSpeciesValues() {
        UUID firstId = UUID.randomUUID();
        insertUser(firstId, "unique-user", "unique@example.com");

        assertThatThrownBy(() ->
                insertUser(UUID.randomUUID(), "unique-user", "other@example.com"))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() ->
                insertUser(UUID.randomUUID(), "other-user", "unique@example.com"))
                .isInstanceOf(DataIntegrityViolationException.class);

        UUID speciesId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO species (id, scientific_name, common_name, family)
                VALUES (?, ?, ?, ?)
                """, speciesId, "Quercus test", "Test oak", "Fagaceae");

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO species (id, scientific_name, common_name, family)
                VALUES (?, ?, ?, ?)
                """, UUID.randomUUID(), "Quercus test", "Another oak", "Fagaceae"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rnF21PostVoteAndCommentReactionUseDatabaseUniqueConstraints() {
        Integer postVoteConstraints = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM pg_constraint
                WHERE conname = 'uk_post_voter' AND contype = 'u'
                """, Integer.class);

        Integer reactionConstraints = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM pg_constraint
                WHERE conname = 'uk_comment_reaction_user' AND contype = 'u'
                """, Integer.class);

        assertThat(postVoteConstraints).isEqualTo(1);
        assertThat(reactionConstraints).isEqualTo(1);
    }

    @Test
    void rnF22EntityPrimaryKeysAreUUIDColumns() {
        Integer uuidPrimaryKeyCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.table_constraints tc
                JOIN information_schema.key_column_usage kcu
                  ON tc.constraint_name = kcu.constraint_name
                 AND tc.table_schema = kcu.table_schema
                JOIN information_schema.columns c
                  ON c.table_schema = kcu.table_schema
                 AND c.table_name = kcu.table_name
                 AND c.column_name = kcu.column_name
                WHERE tc.constraint_type = 'PRIMARY KEY'
                  AND tc.table_name IN ('users', 'species', 'specimens', 'observations', 'posts', 'comments')
                  AND c.data_type = 'uuid'
                """, Integer.class);

        assertThat(uuidPrimaryKeyCount).isGreaterThanOrEqualTo(6);
    }

    private void insertUser(UUID id, String username, String email) {
        jdbcTemplate.update("""
                INSERT INTO users
                    (id, username, email, password_hash, first_name, last_name, gender, role, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
                """, id, username, email, "bcrypt-placeholder", "Test", "User", "OTHER", "USER");
    }
}
