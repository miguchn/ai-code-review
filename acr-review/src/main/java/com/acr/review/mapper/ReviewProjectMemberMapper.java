package com.acr.review.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.acr.review.domain.ReviewProjectMember;

/** 项目成员数据访问。 */
public interface ReviewProjectMemberMapper
{
    List<ReviewProjectMember> selectByProjectId(Long projectId);

    ReviewProjectMember selectByProjectAndUser(@Param("projectId") Long projectId,
                                                @Param("userId") Long userId);

    ReviewProjectMember selectByMemberId(Long memberId);

    int insertReviewProjectMember(ReviewProjectMember member);

    int updateReviewProjectMember(ReviewProjectMember member);

    int deleteReviewProjectMemberById(Long memberId);
}
