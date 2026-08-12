package com.acr.review.service;

import java.util.List;
import com.acr.review.domain.ReviewProjectMember;

public interface IReviewProjectMemberService
{
    List<ReviewProjectMember> selectProjectMembers(Long projectId);

    int saveProjectMember(Long projectId, ReviewProjectMember member);

    int deleteProjectMember(Long projectId, Long memberId);
}
