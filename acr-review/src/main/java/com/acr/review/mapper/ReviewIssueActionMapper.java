package com.acr.review.mapper;

import java.util.List;
import com.acr.review.domain.ReviewIssueAction;

public interface ReviewIssueActionMapper
{
    List<ReviewIssueAction> selectByIssueId(Long issueId);

    int insertAction(ReviewIssueAction action);
}
