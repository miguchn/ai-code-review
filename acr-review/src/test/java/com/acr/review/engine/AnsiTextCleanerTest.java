package com.acr.review.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AnsiTextCleanerTest
{
    @Test
    void stripsColorCodesFromPreviewText()
    {
        String colored = "Preview: 1 file(s) changed  |  \u001b[32m+1\u001b[0m  \u001b[31m-1\u001b[0m\n"
            + "\n\u001b[1mWill review (1):\u001b[0m\n"
            + "  \u001b[33m[M]\u001b[0m  SampleService.java   \u001b[32m+1   \u001b[0m \u001b[31m-1   \u001b[0m";

        String cleaned = AnsiTextCleaner.strip(colored);

        assertFalse(cleaned.contains("\u001b"));
        assertFalse(cleaned.contains("[32m"));
        assertFalse(cleaned.contains("[0m"));
        assertEquals(true, cleaned.contains("Preview: 1 file(s) changed"));
        assertEquals(true, cleaned.contains("Will review (1):"));
        assertEquals(true, cleaned.contains("[M]  SampleService.java"));
        assertEquals(true, cleaned.contains("+1"));
        assertEquals(true, cleaned.contains("-1"));
    }

    @Test
    void parserStoresCleanMessageForTextPreview()
    {
        ReviewEngineOutputParser parser = new ReviewEngineOutputParser();
        String colored = "\u001b[1mWill review (1):\u001b[0m SampleService.java";

        Map<String, Object> structured = parser.parse(colored, ReviewEngineInvocationType.REVIEW_PREVIEW);

        assertEquals("Will review (1): SampleService.java", structured.get("message"));
    }

    @Test
    void returnsNullUnchanged()
    {
        assertNull(AnsiTextCleaner.strip(null));
    }
}
