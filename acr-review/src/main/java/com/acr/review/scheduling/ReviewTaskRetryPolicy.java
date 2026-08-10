package com.acr.review.scheduling;

import java.util.Set;
import org.springframework.stereotype.Component;
import com.acr.review.domain.ReviewPipelineConstants;

/** 将稳定错误码映射为自动重试或人工处置，避免按异常文案判断。 */
@Component
public class ReviewTaskRetryPolicy
{
    private static final Set<String> RETRYABLE_ERRORS = Set.of(
        ReviewPipelineConstants.FAILURE_TIMEOUT,
        ReviewPipelineConstants.FAILURE_RATE_LIMIT,
        ReviewPipelineConstants.FAILURE_DEPENDENCY_UNAVAILABLE,
        ReviewPipelineConstants.FAILURE_MODEL,
        ReviewPipelineConstants.FAILURE_ENGINE,
        ReviewPipelineConstants.FAILURE_CONCURRENCY);

    private final ReviewTaskRuntimeSettings settings;

    public ReviewTaskRetryPolicy(ReviewTaskRuntimeSettings settings)
    {
        this.settings = settings;
    }

    public RetryDecision decide(String errorCode, Integer completedRetries)
    {
        int retries = completedRetries == null ? 0 : Math.max(0, completedRetries);
        if (!RETRYABLE_ERRORS.contains(errorCode) || retries >= settings.maxRetries())
        {
            return RetryDecision.terminal(retries);
        }
        return RetryDecision.retry(retries + 1, settings.retryDelaySeconds(retries));
    }

    public record RetryDecision(boolean retry, int retryCount, int delaySeconds)
    {
        private static RetryDecision retry(int retryCount, int delaySeconds)
        {
            return new RetryDecision(true, retryCount, delaySeconds);
        }

        private static RetryDecision terminal(int retryCount)
        {
            return new RetryDecision(false, retryCount, 0);
        }
    }
}
