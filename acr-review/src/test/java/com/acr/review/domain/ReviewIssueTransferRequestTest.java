package com.acr.review.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import com.acr.common.exception.ServiceException;

class ReviewIssueTransferRequestTest
{
    @Test
    void parseAssigneeUserId_acceptsNumberAndNumericString()
    {
        assertEquals(22L, ReviewIssueTransferRequest.parseAssigneeUserId(22));
        assertEquals(22L, ReviewIssueTransferRequest.parseAssigneeUserId(22L));
        assertEquals(22L, ReviewIssueTransferRequest.parseAssigneeUserId("22"));
        assertNull(ReviewIssueTransferRequest.parseAssigneeUserId(null));
        assertNull(ReviewIssueTransferRequest.parseAssigneeUserId("  "));
    }

    @Test
    void parseAssigneeUserId_rejectsNonNumericWithHumanMessage()
    {
        ServiceException ex = assertThrows(ServiceException.class,
            () -> ReviewIssueTransferRequest.parseAssigneeUserId("张三"));
        assertEquals("责任人 ID 无效", ex.getMessage());
    }

    @Test
    void setter_rejectsNonNumericWithHumanMessage()
    {
        ReviewIssueTransferRequest request = new ReviewIssueTransferRequest();
        ServiceException ex = assertThrows(ServiceException.class, () -> request.setAssigneeUserId("abc"));
        assertEquals("责任人 ID 无效", ex.getMessage());
    }
}
