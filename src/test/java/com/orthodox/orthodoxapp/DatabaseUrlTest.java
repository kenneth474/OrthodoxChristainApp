package com.orthodox.orthodoxapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Checks that DatabaseUrl turns a host's DATABASE_URL into the settings
 * Spring Boot needs, and refuses addresses it can't use.
 */
class DatabaseUrlTest {

    @Test
    void splitsAnAddressLikeRendersIntoJdbcSettings() {
        Map<String, String> settings = DatabaseUrl.toSettings(
                "postgresql://orthodoxapp:s3cret@dpg-abc123-a:5432/orthodoxapp");

        assertThat(settings).containsExactly(
                Map.entry("app.database.url", "jdbc:postgresql://dpg-abc123-a:5432/orthodoxapp"),
                Map.entry("app.database.username", "orthodoxapp"),
                Map.entry("app.database.password", "s3cret"));
    }

    @Test
    void usesTheUsualPortAndKeepsOptions() {
        Map<String, String> settings = DatabaseUrl.toSettings(
                "postgres://me:pw@db.example.com/daily?sslmode=require");

        assertThat(settings.get("app.database.url"))
                .isEqualTo("jdbc:postgresql://db.example.com:5432/daily?sslmode=require");
    }

    @Test
    void decodesEscapedCharactersInThePassword() {
        // A password containing "@" and ":" has to be written as %40 and %3A in the address.
        Map<String, String> settings = DatabaseUrl.toSettings("postgresql://me:p%40ss%3Aword@localhost/daily");

        assertThat(settings.get("app.database.password")).isEqualTo("p@ss:word");
    }

    @Test
    void refusesOtherKindsOfAddressWithoutShowingThePassword() {
        assertThatThrownBy(() -> DatabaseUrl.toSettings("mysql://me:topsecret@localhost/daily"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageNotContaining("topsecret");
        assertThatThrownBy(() -> DatabaseUrl.toSettings("postgresql://localhost/daily"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
