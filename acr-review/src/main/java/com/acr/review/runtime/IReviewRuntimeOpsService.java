package com.acr.review.runtime;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 运行概览与积压处置用例。 */
public interface IReviewRuntimeOpsService
{
    ReviewRuntimeOverview getOverview();

    List<ReviewRuntimeBacklogItem> listOverduePendingTasks(Integer limit);

    List<ReviewRuntimeBacklogItem> listLeaseExpiredTasks(Integer limit);

    List<ReviewRuntimeBacklogItem> listStuckDeliveries(Integer limit);

    Map<String, Integer> currentAlertThresholds();
}
