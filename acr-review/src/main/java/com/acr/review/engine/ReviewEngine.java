package com.acr.review.engine;

/** 审查引擎契约。 */
public interface ReviewEngine
{
    ReviewEngineResult execute(ReviewEngineRequest request);
}
