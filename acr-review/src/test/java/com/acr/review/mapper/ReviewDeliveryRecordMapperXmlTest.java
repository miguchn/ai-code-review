package com.acr.review.mapper;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.InputStream;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class ReviewDeliveryRecordMapperXmlTest
{
    @Test
    void parsesPersistentIntentLeaseAndRetryStatements()
    {
        Configuration configuration = new Configuration();
        String resource = "mapper/review/ReviewDeliveryRecordMapper.xml";
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource))
        {
            assertTrue(input != null, "ReviewDeliveryRecordMapper.xml must exist");
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        catch (Exception ex)
        {
            throw new AssertionError("ReviewDeliveryRecordMapper.xml must be valid MyBatis XML", ex);
        }

        String namespace = ReviewDeliveryRecordMapper.class.getName() + ".";
        assertTrue(configuration.hasStatement(namespace + "upsertDeliveryIntent"));
        assertTrue(configuration.hasStatement(namespace + "claimDelivery"));
        assertTrue(configuration.hasStatement(namespace + "completeDelivery"));
        assertTrue(configuration.hasStatement(namespace + "failDelivery"));
        assertTrue(configuration.hasStatement(namespace + "requeueDelivery"));
    }
}
