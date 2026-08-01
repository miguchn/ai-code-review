package com.acr.review.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.acr.review.domain.ReviewProject;

/** 代码项目数据访问。 */
public interface ReviewProjectMapper
{
    ReviewProject selectReviewProjectById(Long projectId);

    List<ReviewProject> selectReviewProjectList(ReviewProject project);

    ReviewProject selectByRepository(@Param("provider") String provider,
                                     @Param("repositoryOwner") String repositoryOwner,
                                     @Param("repositoryName") String repositoryName,
                                     @Param("excludeProjectId") Long excludeProjectId);

    int insertReviewProject(ReviewProject project);

    int updateReviewProject(ReviewProject project);

    int updateProjectStatus(@Param("projectId") Long projectId,
                            @Param("status") String status,
                            @Param("updateBy") String updateBy);

    int updateConnectionCheck(ReviewProject project);

    int updateRepositorySync(ReviewProject project);

    /** 更新最近 Webhook 接收结果（Webhook 处理内部调用，不触碰其他字段）。 */
    int updateLastWebhook(@Param("projectId") Long projectId, @Param("lastWebhookResult") String lastWebhookResult);

    int deleteReviewProjectByIds(Long[] projectIds);
}
