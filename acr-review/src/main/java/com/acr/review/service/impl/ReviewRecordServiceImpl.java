package com.acr.review.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import com.acr.common.annotation.DataScope;
import com.acr.common.exception.ServiceException;
import com.acr.review.domain.ReviewDeliveryRecord;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskDetail;
import com.acr.review.domain.ReviewTaskRun;
import com.acr.review.domain.result.ReviewConclusionDailyStat;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.mapper.ReviewTaskMapper;
import com.acr.review.mapper.ReviewTaskRunMapper;
import com.acr.review.service.IReviewDeliveryService;
import com.acr.review.service.IReviewIssueService;
import com.acr.review.service.IReviewRecordService;
import com.acr.review.service.ReviewProjectAccessService;
import com.acr.review.insight.TokenCostCalculator;
import com.acr.system.domain.SysAiModelConfig;
import com.acr.system.service.ISysAiModelConfigService;
import com.acr.system.service.ISysDeptService;

/** 审查记录查询：已结束任务（SUCCESS + FAILED），数据范围与审查任务一致。 */
@Service
public class ReviewRecordServiceImpl implements IReviewRecordService
{
    private final ReviewTaskMapper taskMapper;
    private final ReviewTaskRunMapper runMapper;
    private final ReviewProjectMapper projectMapper;
    private final ISysDeptService deptService;
    private final IReviewDeliveryService deliveryService;
    private final IReviewIssueService issueService;
    private final ReviewProjectAccessService projectAccessService;
    private final ISysAiModelConfigService modelConfigService;

    public ReviewRecordServiceImpl(ReviewTaskMapper taskMapper,
                                   ReviewTaskRunMapper runMapper,
                                   ReviewProjectMapper projectMapper,
                                   ISysDeptService deptService,
                                   IReviewDeliveryService deliveryService,
                                   IReviewIssueService issueService,
                                   ReviewProjectAccessService projectAccessService,
                                   ISysAiModelConfigService modelConfigService)
    {
        this.taskMapper = taskMapper;
        this.runMapper = runMapper;
        this.projectMapper = projectMapper;
        this.deptService = deptService;
        this.deliveryService = deliveryService;
        this.issueService = issueService;
        this.projectAccessService = projectAccessService;
        this.modelConfigService = modelConfigService;
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "owner", permission = "review:record:list")
    public List<ReviewTask> selectReviewRecordList(ReviewTask query)
    {
        projectAccessService.applyQueryScope(query);
        return taskMapper.selectReviewRecordList(query);
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "owner", permission = "review:record:list")
    public int countReviewRecordList(ReviewTask query)
    {
        projectAccessService.applyQueryScope(query);
        return taskMapper.countReviewRecordList(query);
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "owner", permission = "review:record:list")
    public List<ReviewConclusionDailyStat> selectReviewConclusionTrend(ReviewTask query)
    {
        projectAccessService.applyQueryScope(query);
        return taskMapper.selectReviewConclusionTrend(query);
    }

    @Override
    public ReviewTaskDetail selectReviewRecordDetail(Long taskId)
    {
        ReviewTask task = taskMapper.selectReviewTaskById(taskId);
        if (task == null)
        {
            throw new ServiceException("审查记录不存在");
        }
        if (!"SUCCESS".equals(task.getTaskStatus()) && !"FAILED".equals(task.getTaskStatus()))
        {
            throw new ServiceException("该任务尚未结束，请到审查任务中查看执行状态");
        }
        projectAccessService.requireView(task.getProjectId());
        List<ReviewTaskRun> runs = runMapper.selectRunsByTaskId(taskId);
        issueService.enrichRuns(runs, task.getProjectId(), task.getPrNumber(),
            ReviewIssueServiceImpl.resolveRefBranch(task));
        enrichTokenCost(runs);
        ReviewDeliveryRecord delivery = deliveryService.selectSummaryDelivery(task.getProjectId(), task.getPrNumber());
        return new ReviewTaskDetail(task, runs, delivery);
    }

    private void enrichTokenCost(List<ReviewTaskRun> runs)
    {
        if (runs == null || runs.isEmpty())
        {
            return;
        }
        for (ReviewTaskRun run : runs)
        {
            if (run.getSnapshotModelId() == null)
            {
                continue;
            }
            SysAiModelConfig config = modelConfigService.selectSysAiModelConfigById(run.getSnapshotModelId());
            run.setEstimatedCost(TokenCostCalculator.toDouble(TokenCostCalculator.estimate(
                run.getInputTokens(), run.getOutputTokens(),
                config == null ? null : config.getInputPricePer1k(),
                config == null ? null : config.getOutputPricePer1k())));
        }
    }
}
