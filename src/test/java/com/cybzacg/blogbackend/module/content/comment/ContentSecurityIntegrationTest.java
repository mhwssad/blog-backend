package com.cybzacg.blogbackend.module.content.comment;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentSecurityIntegrationTest {
    @Test
    void applicationYamlShouldOnlyExposePublicReadEndpoints() throws IOException {
        String content = Files.readString(Path.of("src/main/resources/application.yml"));

        assertTrue(content.contains("- /api/articles"));
        assertTrue(content.contains("- /api/articles/**"));
        assertTrue(content.contains("- /api/categories/tree"));
        assertTrue(content.contains("- /api/tags"));
        assertTrue(content.contains("- /api/comments"));
        assertFalse(content.contains("/api/user/**"));
        assertFalse(content.contains("/api/sys/**"));
    }

    @Test
    void permissionSqlShouldKeepContentMenuIdsWithinReservedRange() throws IOException {
        String sql = Files.readString(Path.of("src/main/resources/mysql/03_06_menu_content_init.sql"));

        assertTrue(sql.contains("content:article:query"));
        assertTrue(sql.contains("content:article:access"));
        assertTrue(sql.contains("content:footprint:delete"));
        assertFalse(sql.contains("(2, 1700)"));
        assertFalse(sql.contains("(2, 1711)"));

        Matcher matcher = Pattern.compile("\\((17\\d{2}),").matcher(sql);
        while (matcher.find()) {
            int menuId = Integer.parseInt(matcher.group(1));
            assertTrue(menuId >= 1700 && menuId <= 1799);
        }
    }
}
