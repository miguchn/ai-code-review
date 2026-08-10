package com.acr.review.mapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class ReviewTaskMapperXmlTest
{
    @Test
    void parsesSchedulingLeaseAndFencingStatements()
    {
        String resource = "mapper/review/ReviewTaskMapper.xml";
        byte[] bytes;
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource))
        {
            assertTrue(input != null, "ReviewTaskMapper.xml must exist");
            bytes = input.readAllBytes();
        }
        catch (Exception ex)
        {
            throw new AssertionError("ReviewTaskMapper.xml must be readable", ex);
        }

        String xml = new String(bytes, StandardCharsets.UTF_8);
        Configuration configuration = new Configuration();
        try
        {
            new XMLMapperBuilder(new ByteArrayInputStream(bytes), configuration, resource,
                configuration.getSqlFragments()).parse();
        }
        catch (Exception ex)
        {
            throw new AssertionError("ReviewTaskMapper.xml must be valid MyBatis XML", ex);
        }

        String namespace = ReviewTaskMapper.class.getName() + ".";
        assertTrue(configuration.hasStatement(namespace + "selectDispatchableTasks"));
        assertTrue(configuration.hasStatement(namespace + "claimTask"));
        assertTrue(configuration.hasStatement(namespace + "deferDispatchableTask"));
        assertTrue(configuration.hasStatement(namespace + "supersedePendingByChangeKey"));
        assertTrue(configuration.hasStatement(namespace + "countNewerTasksByChangeKey"));
        assertTrue(configuration.hasStatement(namespace + "renewTaskLease"));
        assertTrue(configuration.hasStatement(namespace + "updateTaskExecution"));

        String dispatchBlock = block(xml, "selectDispatchableTasks");
        assertTrue(dispatchBlock.contains("'PENDING'"));
        assertTrue(dispatchBlock.contains("'RETRYING'"));
        assertTrue(dispatchBlock.contains("SUPERSEDED"), "扫描须显式排除 SUPERSEDED");

        String claimBlock = block(xml, "claimTask");
        assertTrue(claimBlock.contains("superseded_by is null"));
        assertTrue(claimBlock.contains("not exists"), "claim 须排除同变更 RUNNING");
        assertTrue(claimBlock.contains("'RUNNING'"));
        assertFalse(claimBlock.toLowerCase().contains("for update"));

        String supersedeBlock = block(xml, "supersedePendingByChangeKey");
        assertTrue(supersedeBlock.contains("'SUPERSEDED'"));
        assertTrue(supersedeBlock.contains("'PENDING'"));
        assertTrue(supersedeBlock.contains("'RETRYING'"));
        assertTrue(supersedeBlock.contains("superseded_by"));
    }

    private static String block(String xml, String id)
    {
        String needle = "id=\"" + id + "\"";
        int start = xml.indexOf(needle);
        assertTrue(start >= 0, "missing statement " + id);
        int cut = xml.length();
        for (String tag : new String[] { "<update id=", "<select id=", "<insert id=" })
        {
            int candidate = xml.indexOf(tag, start + needle.length());
            if (candidate > start && candidate < cut)
            {
                cut = candidate;
            }
        }
        return xml.substring(start, cut);
    }
}
