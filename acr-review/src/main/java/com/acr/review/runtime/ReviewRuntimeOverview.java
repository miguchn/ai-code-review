package com.acr.review.runtime;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.acr.review.scheduling.ReviewRuntimeStatus;

/** 运行概览聚合响应（全部中文标签由前端/接口字段说明承载）。 */
public class ReviewRuntimeOverview
{
    private TaskSurface task = new TaskSurface();
    private ResourceSurface resource = new ResourceSurface();
    private DeliverySurface delivery = new DeliverySurface();
    private List<ReviewRuntimeAlert> alerts = new ArrayList<>();
    private Map<String, Integer> alertThresholds = new LinkedHashMap<>();

    public TaskSurface getTask()
    {
        return task;
    }

    public void setTask(TaskSurface task)
    {
        this.task = task;
    }

    public ResourceSurface getResource()
    {
        return resource;
    }

    public void setResource(ResourceSurface resource)
    {
        this.resource = resource;
    }

    public DeliverySurface getDelivery()
    {
        return delivery;
    }

    public void setDelivery(DeliverySurface delivery)
    {
        this.delivery = delivery;
    }

    public List<ReviewRuntimeAlert> getAlerts()
    {
        return alerts;
    }

    public void setAlerts(List<ReviewRuntimeAlert> alerts)
    {
        this.alerts = alerts;
    }

    public Map<String, Integer> getAlertThresholds()
    {
        return alertThresholds;
    }

    public void setAlertThresholds(Map<String, Integer> alertThresholds)
    {
        this.alertThresholds = alertThresholds;
    }

    public static class TaskSurface
    {
        private long pendingCount;
        private long retryingCount;
        private long runningCount;
        private long supersededCount;
        private Long oldestPendingAgeSeconds;
        private Long oldestPendingTaskId;
        private Map<String, Long> terminalRatio24h = new LinkedHashMap<>();
        private long retryCount24h;
        private long timeoutCount24h;

        public long getPendingCount()
        {
            return pendingCount;
        }

        public void setPendingCount(long pendingCount)
        {
            this.pendingCount = pendingCount;
        }

        public long getRetryingCount()
        {
            return retryingCount;
        }

        public void setRetryingCount(long retryingCount)
        {
            this.retryingCount = retryingCount;
        }

        public long getRunningCount()
        {
            return runningCount;
        }

        public void setRunningCount(long runningCount)
        {
            this.runningCount = runningCount;
        }

        public long getSupersededCount()
        {
            return supersededCount;
        }

        public void setSupersededCount(long supersededCount)
        {
            this.supersededCount = supersededCount;
        }

        public Long getOldestPendingAgeSeconds()
        {
            return oldestPendingAgeSeconds;
        }

        public void setOldestPendingAgeSeconds(Long oldestPendingAgeSeconds)
        {
            this.oldestPendingAgeSeconds = oldestPendingAgeSeconds;
        }

        public Long getOldestPendingTaskId()
        {
            return oldestPendingTaskId;
        }

        public void setOldestPendingTaskId(Long oldestPendingTaskId)
        {
            this.oldestPendingTaskId = oldestPendingTaskId;
        }

        public Map<String, Long> getTerminalRatio24h()
        {
            return terminalRatio24h;
        }

        public void setTerminalRatio24h(Map<String, Long> terminalRatio24h)
        {
            this.terminalRatio24h = terminalRatio24h;
        }

        public long getRetryCount24h()
        {
            return retryCount24h;
        }

        public void setRetryCount24h(long retryCount24h)
        {
            this.retryCount24h = retryCount24h;
        }

        public long getTimeoutCount24h()
        {
            return timeoutCount24h;
        }

        public void setTimeoutCount24h(long timeoutCount24h)
        {
            this.timeoutCount24h = timeoutCount24h;
        }
    }

    public static class ResourceSurface
    {
        private int reviewQueueDepth;
        private int reviewActiveCount;
        private int reviewPoolSize;
        private int reviewQueueCapacity;
        private long reviewRejectedCount;
        private int deliveryQueueDepth;
        private int deliveryActiveCount;
        private int deliveryPoolSize;
        private int deliveryQueueCapacity;
        private long deliveryRejectedCount;
        private int workspaceHeld;
        private int workspaceLimit;
        private long workspaceUsedMb;
        private int workspaceDiskLimitMb;
        private int ocrHeld;
        private int ocrLimit;
        private int llmHeld;
        private int llmLimit;
        private int projectHeldTotal;
        private int projectLimitPerProject;

        public static ResourceSurface from(ReviewRuntimeStatus status)
        {
            ResourceSurface surface = new ResourceSurface();
            if (status == null)
            {
                return surface;
            }
            surface.reviewQueueDepth = status.reviewQueueDepth();
            surface.reviewActiveCount = status.reviewActiveCount();
            surface.reviewPoolSize = status.reviewPoolSize();
            surface.reviewQueueCapacity = status.reviewQueueCapacity();
            surface.reviewRejectedCount = status.reviewRejectedCount();
            surface.deliveryQueueDepth = status.deliveryQueueDepth();
            surface.deliveryActiveCount = status.deliveryActiveCount();
            surface.deliveryPoolSize = status.deliveryPoolSize();
            surface.deliveryQueueCapacity = status.deliveryQueueCapacity();
            surface.deliveryRejectedCount = status.deliveryRejectedCount();
            if (status.budgets() != null)
            {
                surface.workspaceHeld = status.budgets().workspaceHeld();
                surface.workspaceLimit = status.budgets().workspaceLimit();
                surface.workspaceUsedMb = status.budgets().workspaceUsedMb();
                surface.workspaceDiskLimitMb = status.budgets().workspaceDiskLimitMb();
                surface.ocrHeld = status.budgets().ocrHeld();
                surface.ocrLimit = status.budgets().ocrLimit();
                surface.llmHeld = status.budgets().llmHeld();
                surface.llmLimit = status.budgets().llmLimit();
                surface.projectHeldTotal = status.budgets().projectHeldTotal();
                surface.projectLimitPerProject = status.budgets().projectLimitPerProject();
            }
            return surface;
        }

        public int getReviewQueueDepth()
        {
            return reviewQueueDepth;
        }

        public void setReviewQueueDepth(int reviewQueueDepth)
        {
            this.reviewQueueDepth = reviewQueueDepth;
        }

        public int getReviewActiveCount()
        {
            return reviewActiveCount;
        }

        public void setReviewActiveCount(int reviewActiveCount)
        {
            this.reviewActiveCount = reviewActiveCount;
        }

        public int getReviewPoolSize()
        {
            return reviewPoolSize;
        }

        public void setReviewPoolSize(int reviewPoolSize)
        {
            this.reviewPoolSize = reviewPoolSize;
        }

        public int getReviewQueueCapacity()
        {
            return reviewQueueCapacity;
        }

        public void setReviewQueueCapacity(int reviewQueueCapacity)
        {
            this.reviewQueueCapacity = reviewQueueCapacity;
        }

        public long getReviewRejectedCount()
        {
            return reviewRejectedCount;
        }

        public void setReviewRejectedCount(long reviewRejectedCount)
        {
            this.reviewRejectedCount = reviewRejectedCount;
        }

        public int getDeliveryQueueDepth()
        {
            return deliveryQueueDepth;
        }

        public void setDeliveryQueueDepth(int deliveryQueueDepth)
        {
            this.deliveryQueueDepth = deliveryQueueDepth;
        }

        public int getDeliveryActiveCount()
        {
            return deliveryActiveCount;
        }

        public void setDeliveryActiveCount(int deliveryActiveCount)
        {
            this.deliveryActiveCount = deliveryActiveCount;
        }

        public int getDeliveryPoolSize()
        {
            return deliveryPoolSize;
        }

        public void setDeliveryPoolSize(int deliveryPoolSize)
        {
            this.deliveryPoolSize = deliveryPoolSize;
        }

        public int getDeliveryQueueCapacity()
        {
            return deliveryQueueCapacity;
        }

        public void setDeliveryQueueCapacity(int deliveryQueueCapacity)
        {
            this.deliveryQueueCapacity = deliveryQueueCapacity;
        }

        public long getDeliveryRejectedCount()
        {
            return deliveryRejectedCount;
        }

        public void setDeliveryRejectedCount(long deliveryRejectedCount)
        {
            this.deliveryRejectedCount = deliveryRejectedCount;
        }

        public int getWorkspaceHeld()
        {
            return workspaceHeld;
        }

        public void setWorkspaceHeld(int workspaceHeld)
        {
            this.workspaceHeld = workspaceHeld;
        }

        public int getWorkspaceLimit()
        {
            return workspaceLimit;
        }

        public void setWorkspaceLimit(int workspaceLimit)
        {
            this.workspaceLimit = workspaceLimit;
        }

        public long getWorkspaceUsedMb()
        {
            return workspaceUsedMb;
        }

        public void setWorkspaceUsedMb(long workspaceUsedMb)
        {
            this.workspaceUsedMb = workspaceUsedMb;
        }

        public int getWorkspaceDiskLimitMb()
        {
            return workspaceDiskLimitMb;
        }

        public void setWorkspaceDiskLimitMb(int workspaceDiskLimitMb)
        {
            this.workspaceDiskLimitMb = workspaceDiskLimitMb;
        }

        public int getOcrHeld()
        {
            return ocrHeld;
        }

        public void setOcrHeld(int ocrHeld)
        {
            this.ocrHeld = ocrHeld;
        }

        public int getOcrLimit()
        {
            return ocrLimit;
        }

        public void setOcrLimit(int ocrLimit)
        {
            this.ocrLimit = ocrLimit;
        }

        public int getLlmHeld()
        {
            return llmHeld;
        }

        public void setLlmHeld(int llmHeld)
        {
            this.llmHeld = llmHeld;
        }

        public int getLlmLimit()
        {
            return llmLimit;
        }

        public void setLlmLimit(int llmLimit)
        {
            this.llmLimit = llmLimit;
        }

        public int getProjectHeldTotal()
        {
            return projectHeldTotal;
        }

        public void setProjectHeldTotal(int projectHeldTotal)
        {
            this.projectHeldTotal = projectHeldTotal;
        }

        public int getProjectLimitPerProject()
        {
            return projectLimitPerProject;
        }

        public void setProjectLimitPerProject(int projectLimitPerProject)
        {
            this.projectLimitPerProject = projectLimitPerProject;
        }
    }

    public static class DeliverySurface
    {
        private long pendingCount;
        private Long oldestPendingAgeSeconds;
        private Long oldestPendingDeliveryId;
        private long manualCount;

        public long getPendingCount()
        {
            return pendingCount;
        }

        public void setPendingCount(long pendingCount)
        {
            this.pendingCount = pendingCount;
        }

        public Long getOldestPendingAgeSeconds()
        {
            return oldestPendingAgeSeconds;
        }

        public void setOldestPendingAgeSeconds(Long oldestPendingAgeSeconds)
        {
            this.oldestPendingAgeSeconds = oldestPendingAgeSeconds;
        }

        public Long getOldestPendingDeliveryId()
        {
            return oldestPendingDeliveryId;
        }

        public void setOldestPendingDeliveryId(Long oldestPendingDeliveryId)
        {
            this.oldestPendingDeliveryId = oldestPendingDeliveryId;
        }

        public long getManualCount()
        {
            return manualCount;
        }

        public void setManualCount(long manualCount)
        {
            this.manualCount = manualCount;
        }
    }
}
