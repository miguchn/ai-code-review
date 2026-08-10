package com.acr.review.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import com.acr.review.mapper.ReviewTaskMapper;

class ReviewChangeKeyGuardTest
{
    @Test
    void missingChangeKeyIsTreatedAsLatestForCompatibility()
    {
        ReviewTaskMapper mapper = mock(ReviewTaskMapper.class);
        ReviewTask task = new ReviewTask();
        task.setTaskId(1L);
        task.setProjectId(2L);
        assertTrue(ReviewChangeKeyGuard.isLatestForChangeKey(mapper, task));
    }

    @Test
    void newerTaskMakesCurrentStale()
    {
        ReviewTaskMapper mapper = mock(ReviewTaskMapper.class);
        ReviewTask task = new ReviewTask();
        task.setTaskId(10L);
        task.setProjectId(2L);
        task.setChangeKey("PR#3");
        when(mapper.countNewerTasksByChangeKey(2L, "PR#3", 10L)).thenReturn(2);
        assertFalse(ReviewChangeKeyGuard.isLatestForChangeKey(mapper, task));
    }

    @Test
    void zeroNewerMeansLatest()
    {
        ReviewTaskMapper mapper = mock(ReviewTaskMapper.class);
        ReviewTask task = new ReviewTask();
        task.setTaskId(10L);
        task.setProjectId(2L);
        task.setChangeKey("PUSH#main");
        when(mapper.countNewerTasksByChangeKey(2L, "PUSH#main", 10L)).thenReturn(0);
        assertTrue(ReviewChangeKeyGuard.isLatestForChangeKey(mapper, task));
    }
}
