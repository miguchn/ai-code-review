package com.acr.review.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import com.acr.review.domain.ReviewDeliveryRecord;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskRun;
import com.acr.review.mapper.ReviewDeliveryRecordMapper;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.service.IReviewNotifyChannelService;
import org.mockito.Mockito;

class ReviewDeliveryIntentServiceInlineTest
{
    private final ReviewDeliveryRecordMapper mapper = Mockito.mock(ReviewDeliveryRecordMapper.class);
    private final ReviewProjectMapper projectMapper = Mockito.mock(ReviewProjectMapper.class);
    private final IReviewNotifyChannelService channelService = Mockito.mock(IReviewNotifyChannelService.class);
    private final ApplicationEventPublisher publisher = Mockito.mock(ApplicationEventPublisher.class);
    private final ReviewDeliveryIntentService service = new ReviewDeliveryIntentService(
        mapper, projectMapper, channelService, publisher);

    @Test
    void filtersMediumLowAndUsesIssueIdempotencyKey()
    {
        ReviewProject project = enabledProject();
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(project);
        when(mapper.selectByIdempotencyKey(any())).thenReturn(null);
        when(mapper.upsertDeliveryIntent(any())).thenAnswer(inv -> {
            inv.<ReviewDeliveryRecord>getArgument(0).setDeliveryId(90L);
            return 1;
        });

        ReviewTaskRun run = runWithIssues("""
            [
              {"issueId":11,"severity":"CRITICAL","filePath":"a.java","startLine":1,"endLine":1,"title":"c"},
              {"issueId":12,"severity":"HIGH","filePath":"b.java","startLine":2,"endLine":2,"title":"h"},
              {"issueId":13,"severity":"MEDIUM","filePath":"c.java","startLine":3,"endLine":3,"title":"m"},
              {"issueId":14,"severity":"LOW","filePath":"d.java","startLine":4,"endLine":4,"title":"l"}
            ]
            """);

        int count = service.enqueueInlineComments(task(), run,
            ReviewDeliveryConstants.TRIGGER_TASK_SUCCESS, "system");

        assertEquals(2, count);
        ArgumentCaptor<ReviewDeliveryRecord> captor = ArgumentCaptor.forClass(ReviewDeliveryRecord.class);
        verify(mapper, times(2)).upsertDeliveryIntent(captor.capture());
        assertEquals("GITHUB:3:11:INLINE_COMMENT", captor.getAllValues().get(0).getIdempotencyKey());
        assertEquals(11L, captor.getAllValues().get(0).getIssueId());
        assertEquals(ReviewDeliveryConstants.CHANNEL_GITHUB_PR_INLINE, captor.getAllValues().get(0).getChannel());
        assertEquals("GITHUB:3:12:INLINE_COMMENT", captor.getAllValues().get(1).getIdempotencyKey());
    }

    @Test
    void pushOrDisabledShortCircuits()
    {
        ReviewTask push = task();
        push.setEventSource(ReviewPipelineConstants.EVENT_SOURCE_PUSH);
        assertEquals(0, service.enqueueInlineComments(push, runWithIssues("[]"),
            ReviewDeliveryConstants.TRIGGER_TASK_SUCCESS, "system"));
        verify(mapper, never()).upsertDeliveryIntent(any());

        ReviewProject disabled = enabledProject();
        disabled.setInlineCommentEnabled("1");
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(disabled);
        assertEquals(0, service.enqueueInlineComments(task(), runWithIssues("""
            [{"issueId":1,"severity":"CRITICAL","filePath":"a.java","endLine":1}]
            """), ReviewDeliveryConstants.TRIGGER_TASK_SUCCESS, "system"));
        verify(mapper, never()).upsertDeliveryIntent(any());
    }

    @Test
    void skipsAlreadySuccessfulIssue()
    {
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(enabledProject());
        ReviewDeliveryRecord existing = new ReviewDeliveryRecord();
        existing.setDeliveryStatus(ReviewDeliveryConstants.STATUS_SUCCESS);
        when(mapper.selectByIdempotencyKey("GITHUB:3:11:INLINE_COMMENT")).thenReturn(existing);

        int count = service.enqueueInlineComments(task(), runWithIssues("""
            [{"issueId":11,"severity":"CRITICAL","filePath":"a.java","endLine":1}]
            """), ReviewDeliveryConstants.TRIGGER_TASK_SUCCESS, "system");

        assertEquals(0, count);
        verify(mapper, never()).upsertDeliveryIntent(any());
    }

    @Test
    void rendererContainsMarkerAndSeverity()
    {
        com.acr.review.domain.result.ReviewTopIssue issue = new com.acr.review.domain.result.ReviewTopIssue();
        issue.setIssueId(52L);
        issue.setSeverity("CRITICAL");
        issue.setCategory("SECURITY");
        issue.setTitle("SQL 注入漏洞");
        issue.setDescription("memberId 未校验");
        issue.setSuggestion("使用 PreparedStatement");
        String body = ReviewInlineCommentRenderer.render(issue, 22L);
        assertTrue(body.contains("🚨"));
        assertTrue(body.contains("严重"));
        assertTrue(body.contains("安全"));
        assertTrue(body.contains("问题 #52"));
        assertTrue(body.contains(ReviewDeliveryConstants.inlineCommentMarker(52L)));
        assertTrue(body.contains("审查记录 #22"));
        assertTrue(body.contains("💡 建议："));
    }

    @Test
    void summaryPreviewAppendsInlineLine()
    {
        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .inlineCommentCount(2)
            .inlineCriticalCount(1)
            .inlineHighCount(1)
            .build();
        String body = ReviewCommentBodyRenderer.render(content);
        assertTrue(body.contains("本次审查生成 2 条行内评论（严重 1 · 高 1），单独发布于代码平台"));
        assertFalse(ReviewCommentBodyRenderer.render(ReviewSummaryContent.builder().build())
            .contains("行内评论"));
    }

    private static ReviewTask task()
    {
        ReviewTask task = new ReviewTask();
        task.setTaskId(10L);
        task.setProjectId(3L);
        task.setProvider("GITHUB");
        task.setPrNumber(8);
        task.setTaskStatus(ReviewPipelineConstants.TASK_SUCCESS);
        task.setEventSource(ReviewPipelineConstants.EVENT_SOURCE_PR);
        return task;
    }

    private static ReviewTaskRun runWithIssues(String json)
    {
        ReviewTaskRun run = new ReviewTaskRun();
        run.setRunId(100L);
        run.setTopIssuesJson(json);
        return run;
    }

    private static ReviewProject enabledProject()
    {
        ReviewProject project = new ReviewProject();
        project.setProjectId(3L);
        project.setProvider("GITHUB");
        project.setInlineCommentEnabled("0");
        project.setInlineSeverities("CRITICAL,HIGH");
        return project;
    }
}
