package com.orthodox.orthodoxapp;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DatabaseUrl: turns the database address a hosting service gives us into
 * the three settings Spring Boot needs.
 *
 * Render (and Railway, Heroku and most others) hand the app its database as
 * one environment variable called DATABASE_URL, written like a web address:
 *
 *     postgresql://USER:PASSWORD@HOST:5432/DATABASE
 *
 * Java's database code (JDBC) wants a different shape, with the user name
 * and password kept separate:
 *
 *     app.database.url      = jdbc:postgresql://HOST:5432/DATABASE
 *     app.database.username = USER
 *     app.database.password = PASSWORD
 *
 * main() calls useIfPresent() just before Spring Boot starts, and
 * application-prod.properties passes these on to spring.datasource.*.
 * On your computer there's no DATABASE_URL, so this does nothing and the
 * app keeps using H2.
 *
 * Because the conversion happens here, moving to another host later only
 * means giving the app a different DATABASE_URL: no code changes.
 */
final class DatabaseUrl {

    private DatabaseUrl() {
        // only static methods; never created
    }

    /** If the DATABASE_URL environment variable is set, store its parts as app.database.* settings. */
    static void useIfPresent() {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl != null && !databaseUrl.isBlank()) {
            // System properties are one of the places Spring Boot reads settings from.
            toSettings(databaseUrl).forEach(System::setProperty);
        }
    }

    /**
     * The conversion itself, kept separate so DatabaseUrlTest can check it
     * without setting environment variables. Error messages never include
     * the address, because it contains the database password.
     */
    static Map<String, String> toSettings(String databaseUrl) {
        URI uri;
        try {
            uri = URI.create(databaseUrl.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("DATABASE_URL isn't a valid address");
        }
        if (!"postgresql".equals(uri.getScheme()) && !"postgres".equals(uri.getScheme())) {
            throw new IllegalArgumentException("DATABASE_URL should start with postgresql://");
        }
        if (uri.getHost() == null || uri.getUserInfo() == null) {
            throw new IllegalArgumentException("DATABASE_URL should look like postgresql://USER:PASSWORD@HOST:5432/DATABASE");
        }

        // "USER:PASSWORD". getUserInfo() also decodes escapes such as %40 back into @.
        String userInfo = uri.getUserInfo();
        int colon = userInfo.indexOf(':');
        String user = colon < 0 ? userInfo : userInfo.substring(0, colon);
        String password = colon < 0 ? "" : userInfo.substring(colon + 1);

        // 5432 is PostgreSQL's usual port, used when the address doesn't name one.
        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        // Keep any options after "?" (such as ?sslmode=require); JDBC understands them too.
        String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();

        Map<String, String> settings = new LinkedHashMap<>();
        settings.put("app.database.url", "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getRawPath() + query);
        settings.put("app.database.username", user);
        settings.put("app.database.password", password);
        return settings;
    }
}
