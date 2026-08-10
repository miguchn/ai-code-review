package com.acr.review.domain;

import com.acr.common.utils.StringUtils;
import com.acr.review.mapper.ReviewTaskMapper;

/**
 * 同一 project_id+change_key 的最新任务围栏：对账与总结评论回写前共用。
 */
public final class ReviewChangeKeyGuard
{
    private ReviewChangeKeyGuard()
    {
    }

    /**
     * 本任务是否仍是该变更键下的最新任务（无更大 task_id）。
     * change_key 缺失时不做围栏（兼容历史行），视为可提交。
     */
    public static boolean isLatestForChangeKey(ReviewTaskMapper taskMapper, ReviewTask task)
    {
        if (taskMapper == null || task == null
            || task.getTaskId() == null
            || task.getProjectId() == null
            || StringUtils.isEmpty(task.getChangeKey()))
        {
            return true;
        }
        return taskMapper.countNewerTasksByChangeKey(
            task.getProjectId(), task.getChangeKey(), task.getTaskId()) == 0;
    }
}
