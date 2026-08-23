package com.acr.review.runtime;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.acr.review.engine.OcrEngineAvailability;

/** 运行概览与积压处置用例。 */
public interface IReviewRuntimeOpsService
{
    ReviewRuntimeOverview getOverview();

    OcrEngineAvailability getOcrEngineAvailability();

    List<ReviewRuntimeBacklogItem> listOverduePendingTasks(Integer limit);

    List<ReviewRuntimeBacklogItem> listLeaseExpiredTasks(Integer limit);

    List<ReviewRuntimeBacklogItem> listStuckDeliveries(Integer limit);

    Map<String, Integer> currentAlertThresholds();
}
