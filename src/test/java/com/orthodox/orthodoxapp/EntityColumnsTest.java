package com.orthodox.orthodoxapp;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Entity;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.ColumnDefault;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 * Guards against the bug that stopped everyone signing in on 2026-10-24.
 *
 * Hibernate adds a new column to an existing table for each new field. A
 * number or true/false field (int, long, boolean...) can't be empty, so its
 * column is "not null". Adding such a column to a table that already has
 * rows fails in PostgreSQL unless the column has a default value for those
 * rows, and every query on the table then breaks. So: every primitive field
 * of every @Entity must say its default with @ColumnDefault.
 */
class EntityColumnsTest {

    @Test
    void everyNumberOrTrueFalseColumnHasADefault() throws Exception {
        // Find every @Entity class in the app.
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        List<String> missing = new ArrayList<>();
        for (BeanDefinition entity : scanner.findCandidateComponents("com.orthodox.orthodoxapp")) {
            for (Field field : Class.forName(entity.getBeanClassName()).getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || !field.getType().isPrimitive()) continue;
                if (field.getAnnotation(ColumnDefault.class) == null) {
                    missing.add(field.getDeclaringClass().getSimpleName() + "." + field.getName());
                }
            }
        }

        assertThat(missing).as("primitive @Entity fields without @ColumnDefault").isEmpty();
    }
}
