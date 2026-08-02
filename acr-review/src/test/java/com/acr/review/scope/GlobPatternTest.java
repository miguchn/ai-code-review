package com.acr.review.scope;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class GlobPatternTest
{
    @Test
    void doubleStarMatchesAnyDepthIncludingRoot()
    {
        assertTrue(GlobPattern.matches("**/package-lock.json", "package-lock.json"));
        assertTrue(GlobPattern.matches("**/package-lock.json", "web/admin/package-lock.json"));
        assertTrue(GlobPattern.matches("**/*Test.java", "src/test/java/com/FooTest.java"));
        assertTrue(GlobPattern.matches("**/*.sql", "01_core.sql"));
        assertTrue(GlobPattern.matches("**/*.sql", "sql/01_core.sql"));
    }

    @Test
    void doubleStarMiddleMatchesMiddleSegments()
    {
        assertTrue(GlobPattern.matches("**/dist/**", "web/dist/app.js"));
        assertTrue(GlobPattern.matches("**/dist/**", "dist/app.js"));
        assertTrue(GlobPattern.matches("sql/**", "sql/nested/01.sql"));
        assertFalse(GlobPattern.matches("sql/**", "other/01.sql"));
    }

    @Test
    void singleStarDoesNotCrossSlash()
    {
        assertTrue(GlobPattern.matches("*.js", "app.js"));
        assertFalse(GlobPattern.matches("*.js", "web/app.js"));
        assertTrue(GlobPattern.matches("web/*.js", "web/app.js"));
        assertFalse(GlobPattern.matches("web/*.js", "web/admin/app.js"));
    }

    @Test
    void literalDotsAreEscaped()
    {
        assertFalse(GlobPattern.matches("**/*.map", "assets/mapp"));
        assertTrue(GlobPattern.matches("**/*.map", "assets/app.map"));
        assertFalse(GlobPattern.matches("**/package.json", "web/packageXjson"));
    }

    @Test
    void normalizesLeadingSlashAndBackslash()
    {
        assertTrue(GlobPattern.matches("**/*.yml", "/config/app.yml"));
        assertTrue(GlobPattern.matches("**/*.yml", "config\\app.yml"));
    }

    @Test
    void blankInputsNeverMatch()
    {
        assertFalse(GlobPattern.matches(null, "a.js"));
        assertFalse(GlobPattern.matches("  ", "a.js"));
        assertFalse(GlobPattern.matches("*.js", null));
    }
}
