package com.acr.review.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import com.acr.review.domain.ReviewPipelineConstants;

class ReviewTaskRetryPolicyTest
{
    private final ReviewTaskRuntimeSettings settings = mock(ReviewTaskRuntimeSettings.class);
    private final ReviewTaskRetryPolicy policy = new ReviewTaskRetryPolicy(settings);

    @Test
    void retriesTransientFailureWithConfiguredBackoff()
    {
        when(settings.maxRetries()).thenReturn(3);
        when(settings.retryDelaySeconds(1)).thenReturn(60);

        ReviewTaskRetryPolicy.RetryDecision decision = policy.decide(
            ReviewPipelineConstants.FAILURE_TIMEOUT, 1);

        assertTrue(decision.retry());
        assertEquals(2, decision.retryCount());
        assertEquals(60, decision.delaySeconds());
    }

    @Test
    void sendsConfigurationFailureDirectlyToManualTerminal()
    {
        when(settings.maxRetries()).thenReturn(3);

        ReviewTaskRetryPolicy.RetryDecision decision = policy.decide(
            ReviewPipelineConstants.FAILURE_CONFIG_MISSING, 0);

        assertFalse(decision.retry());
        assertEquals(0, decision.retryCount());
    }

    @Test
    void stopsRetryingAtConfiguredLimit()
    {
        when(settings.maxRetries()).thenReturn(3);

        ReviewTaskRetryPolicy.RetryDecision decision = policy.decide(
            ReviewPipelineConstants.FAILURE_RATE_LIMIT, 3);

        assertFalse(decision.retry());
        assertEquals(3, decision.retryCount());
    }
}
