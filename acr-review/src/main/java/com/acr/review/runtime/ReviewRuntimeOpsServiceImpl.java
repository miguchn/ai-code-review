package com.acr.review.runtime;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.acr.review.delivery.ReviewDeliveryConstants;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.mapper.ReviewRuntimeStatsMapper;
import com.acr.review.scheduling.IReviewRuntimeStatusService;

@Service
public class ReviewRuntimeOpsServiceImpl implements IReviewRuntimeOpsService
{
    private static final int DEFAULT_BACKLOG_LIMIT = 50;

    private final ReviewRuntimeStatsMapper statsMapper;
    private final IReviewRuntimeStatusService runtimeStatusService;
    private final ReviewRuntimeAlertService alertService;
    private final ReviewRuntimeAlertSettings settings;

    public ReviewRuntimeOpsServiceImpl(ReviewRuntimeStatsMapper statsMapper,
                                       IReviewRuntimeStatusService runtimeStatusService,
                                       ReviewRuntimeAlertService alertService,
                                       ReviewRuntimeAlertSettings settings)
    {
        this.statsMapper = statsMapper;
        this.runtimeStatusService = runtimeStatusService;
        this.alertService = alertService;
        this.settings = settings;
    }

    @Override
    public ReviewRuntimeOverview getOverview()
    {
        ReviewRuntimeOverview overview = new ReviewRuntimeOverview();
        overview.setTask(buildTaskSurface());
        overview.setResource(ReviewRuntimeOverview.ResourceSurface.from(runtimeStatusService.snapshot()));
        overview.setDelivery(buildDeliverySurface());
        overview.setAlerts(alertService.evaluateNow());
        overview.setAlertThresholds(currentAlertThresholds());
        return overview;
    }

    @Override
    public List<ReviewRuntimeBacklogItem> listOverduePendingTasks(Integer limit)
    {
        return statsMapper.selectOverduePendingTasks(settings.pendingAgeMinutes(), resolveLimit(limit));
    }

    @Override
    public List<ReviewRuntimeBacklogItem> listLeaseExpiredTasks(Integer limit)
    {
        return statsMapper.selectLeaseExpiredTasks(resolveLimit(limit));
    }

    @Override
    public List<ReviewRuntimeBacklogItem> listStuckDeliveries(Integer limit)
    {
        return statsMapper.selectStuckDeliveries(settings.deliveryPendingAgeMinutes(), resolveLimit(limit));
    }

    @Override
    public Map<String, Integer> currentAlertThresholds()
    {
        Map<String, Integer> thresholds = new LinkedHashMap<>();
        thresholds.put("pendingAgeMinutes", settings.pendingAgeMinutes());
        thresholds.put("deliveryPendingAgeMinutes", settings.deliveryPendingAgeMinutes());
        thresholds.put("budgetSaturatedMinutes", settings.budgetSaturatedMinutes());
        thresholds.put("failureRateWindowMinutes", settings.failureRateWindowMinutes());
        thresholds.put("failureRatePercent", settings.failureRatePercent());
        thresholds.put("drainTimeoutSeconds", settings.drainTimeoutSeconds());
        return thresholds;
    }

    private ReviewRuntimeOverview.TaskSurface buildTaskSurface()
    {
        ReviewRuntimeOverview.TaskSurface task = new ReviewRuntimeOverview.TaskSurface();
        Map<String, Long> counts = toCountMap(statsMapper.countTasksByStatus(List.of(
            ReviewPipelineConstants.TASK_PENDING,
            ReviewPipelineConstants.TASK_RETRYING,
            ReviewPipelineConstants.TASK_RUNNING,
            ReviewPipelineConstants.TASK_SUPERSEDED)));
        task.setPendingCount(counts.getOrDefault(ReviewPipelineConstants.TASK_PENDING, 0L));
        task.setRetryingCount(counts.getOrDefault(ReviewPipelineConstants.TASK_RETRYING, 0L));
        task.setRunningCount(counts.getOrDefault(ReviewPipelineConstants.TASK_RUNNING, 0L));
        task.setSupersededCount(counts.getOrDefault(ReviewPipelineConstants.TASK_SUPERSEDED, 0L));

        Map<String, Object> oldest = statsMapper.selectOldestPendingTask();
        if (oldest != null)
        {
            task.setOldestPendingTaskId(toLongObject(oldest.get("taskId")));
            task.setOldestPendingAgeSeconds(toLongObject(oldest.get("ageSeconds")));
        }

        Map<String, Long> terminals = toCountMap(statsMapper.countTerminalTasksSinceHours(24));
        Map<String, Long> ratio = new LinkedHashMap<>();
        ratio.put("成功", terminals.getOrDefault(ReviewPipelineConstants.TASK_SUCCESS, 0L));
        ratio.put("已失败", terminals.getOrDefault(ReviewPipelineConstants.TASK_FAILED, 0L));
        ratio.put("已取消", terminals.getOrDefault(ReviewPipelineConstants.TASK_CANCELLED, 0L));
        ratio.put("已被替代", terminals.getOrDefault(ReviewPipelineConstants.TASK_SUPERSEDED, 0L));
        task.setTerminalRatio24h(ratio);
        task.setRetryCount24h(statsMapper.countRetryEventsSinceHours(24));
        task.setTimeoutCount24h(statsMapper.countTimeoutEventsSinceHours(24));
        return task;
    }

    private ReviewRuntimeOverview.DeliverySurface buildDeliverySurface()
    {
        ReviewRuntimeOverview.DeliverySurface delivery = new ReviewRuntimeOverview.DeliverySurface();
        delivery.setPendingCount(statsMapper.countDeliveriesByStatus(ReviewDeliveryConstants.STATUS_PENDING));
        delivery.setManualCount(statsMapper.countDeliveriesByStatus(ReviewDeliveryConstants.STATUS_MANUAL));
        Map<String, Object> oldest = statsMapper.selectOldestPendingDelivery();
        if (oldest != null)
        {
            delivery.setOldestPendingDeliveryId(toLongObject(oldest.get("deliveryId")));
            delivery.setOldestPendingAgeSeconds(toLongObject(oldest.get("ageSeconds")));
        }
        return delivery;
    }

    private static Map<String, Long> toCountMap(List<Map<String, Object>> rows)
    {
        Map<String, Long> map = new LinkedHashMap<>();
        if (rows == null)
        {
            return map;
        }
        for (Map<String, Object> row : rows)
        {
            if (row == null || row.get("status") == null)
            {
                continue;
            }
            map.put(String.valueOf(row.get("status")), toLong(row.get("cnt")));
        }
        return map;
    }

    private static int resolveLimit(Integer limit)
    {
        if (limit == null || limit <= 0)
        {
            return DEFAULT_BACKLOG_LIMIT;
        }
        return Math.min(limit, 200);
    }

    private static long toLong(Object value)
    {
        if (value == null)
        {
            return 0L;
        }
        if (value instanceof Number number)
        {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private static Long toLongObject(Object value)
    {
        if (value == null)
        {
            return null;
        }
        return toLong(value);
    }
}
