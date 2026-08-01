package com.acr.review.service;

import java.util.List;
import com.acr.review.domain.GitRepositoryReadRequest;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewProjectOptions;
import com.acr.review.domain.ReviewRepositoryInfo;
import com.acr.review.git.GitConnectionResult;

/** 代码项目用例。 */
public interface IReviewProjectService
{
    ReviewProject selectReviewProjectById(Long projectId);

    List<ReviewProject> selectReviewProjectList(ReviewProject project);

    ReviewProjectOptions getFormOptions();

    ReviewRepositoryInfo readRepositoryInfo(GitRepositoryReadRequest request);

    int insertReviewProject(ReviewProject project);

    int updateReviewProject(ReviewProject project);

    void deleteReviewProjectByIds(Long[] projectIds);

    int updateProjectStatus(Long projectId, String status);

    GitConnectionResult testConnection(Long projectId);
}
