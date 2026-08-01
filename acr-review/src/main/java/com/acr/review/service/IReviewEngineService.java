package com.acr.review.service;

import com.acr.review.domain.ReviewEngineTestRequest;
import com.acr.review.engine.ReviewEngineInfo;
import com.acr.review.engine.ReviewEngineResult;

/** 审查引擎管理用例。 */
public interface IReviewEngineService
{
    ReviewEngineInfo getEngineInfo();

    ReviewEngineResult detectEnvironment();

    ReviewEngineResult testInvoke(ReviewEngineTestRequest request);
}
