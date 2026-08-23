package com.acr.review.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Date;
import java.util.List;
import com.acr.review.domain.ReviewIssue;
import com.acr.review.mapper.ReviewIssueMapper;
import com.acr.system.service.ISysUserIdentityService;
import org.junit.jupiter.api.Test;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskRun;
import com.acr.system.service.ISysConfigService;

/** 总结评论中合并请求 Web 链接的平台路径规则（§7.3）。 */
class ReviewSummaryContentFactoryTest
{
    @Test
    void assemblesCommitMessageSummaryAndReviewTimeFromRun()
    {
        ReviewTask task = new ReviewTask();
        task.setTaskId(1L);
        task.setPrNumber(4);
        ReviewTaskRun run = new ReviewTaskRun();
        run.setCommitMessages("Fix login\nSecond line");
        run.setResultSummary("整体风险可控");
        Date finished = new Date(1_700_000_000_000L);
        run.setFinishedTime(finished);
        ReviewProject project = project("GITHUB", "https://github.com/acme/demo", "acme", "demo");
        project.setProjectName("Demo 项目");
        project.setBusinessSystemName("长寿官网系统");

        ReviewSummaryContent content = new ReviewSummaryContentFactory(mock(ISysConfigService.class))
            .build(task, run, project);

        assertEquals("Fix login", content.getCommitMessage());
        assertEquals("整体风险可控", content.getSummaryText());
        assertEquals(finished, content.getReviewTime());
        assertEquals("Demo 项目", content.getProjectName());
        assertEquals("长寿官网系统", content.getBusinessSystemName());
        assertTrue(content.getAssignees().isEmpty());
    }

    @Test
    void githubUsesPullPath()
    {
        ReviewProject project = project("GITHUB", "https://github.com/acme/demo", "acme", "demo");
        assertEquals("https://github.com/acme/demo/pull/8",
            ReviewSummaryContentFactory.buildMergeRequestUrl(project, 8));
    }

    @Test
    void gitlabUsesMergeRequestsPath()
    {
        ReviewProject project = project("GITLAB", "https://gitlab.example.com/group/sub/demo", "group/sub", "demo");
        assertEquals("https://gitlab.example.com/group/sub/demo/-/merge_requests/8",
            ReviewSummaryContentFactory.buildMergeRequestUrl(project, 8));
    }

    @Test
    void giteeUsesPullsPath()
    {
        ReviewProject project = project("GITEE", "https://gitee.com/acme/demo", "acme", "demo");
        assertEquals("https://gitee.com/acme/demo/pulls/8",
            ReviewSummaryContentFactory.buildMergeRequestUrl(project, 8));
    }

    @Test
    void giteaUsesPullsPath()
    {
        ReviewProject project = project("GITEA", "https://gitea.example.com/acme/demo", "acme", "demo");
        assertEquals("https://gitea.example.com/acme/demo/pulls/8",
            ReviewSummaryContentFactory.buildMergeRequestUrl(project, 8));
    }

    @Test
    void githubFallsBackToOwnerAndNameWhenRepositoryUrlMissing()
    {
        ReviewProject project = project("GITHUB", null, "acme", "demo");
        assertEquals("https://github.com/acme/demo/pull/8",
            ReviewSummaryContentFactory.buildMergeRequestUrl(project, 8));
    }

    @Test
    void stripsGitSuffixAndTrailingSlash()
    {
        ReviewProject project = project("GITHUB", "https://github.com/acme/demo.git/", "acme", "demo");
        assertEquals("https://github.com/acme/demo/pull/8",
            ReviewSummaryContentFactory.buildMergeRequestUrl(project, 8));
    }

    @Test
    void collectAssignees_resolvesImIdentities()
    {
        ReviewIssue issue = new ReviewIssue();
        issue.setAssigneeUserId(3L);
        issue.setAssigneeName("张三");
        com.acr.system.domain.SysUserIdentity ding = new com.acr.system.domain.SysUserIdentity();
        ding.setIdentityType(com.acr.system.domain.SysUserIdentity.TYPE_IM_DINGTALK);
        ding.setIdentifier("13800000000");
        ISysUserIdentityService identities = mock(ISysUserIdentityService.class);
        when(identities.listByUserId(3L)).thenReturn(List.of(ding));

        ReviewAssigneeMention mention = new ReviewSummaryContentFactory(
            mock(ISysConfigService.class), null, identities)
            .collectAssignees(List.of(issue)).get(0);
        assertEquals("张三", mention.getDisplayName());
        assertEquals("13800000000", mention.getDingTalkMobile());
    }

    @Test
    void build_assigneeLookupFailureDoesNotThrow()
    {
        ReviewTask task = new ReviewTask();
        task.setTaskId(1L);
        ReviewIssueMapper issueMapper = mock(ReviewIssueMapper.class);
        when(issueMapper.selectAssignedByLastTaskId(1L)).thenThrow(new RuntimeException("lookup failed"));

        ReviewSummaryContent content = new ReviewSummaryContentFactory(
            mock(ISysConfigService.class), issueMapper, mock(ISysUserIdentityService.class))
            .build(task, new ReviewTaskRun(), project("GITHUB", "https://github.com/acme/demo", "acme", "demo"));
        assertTrue(content.getAssignees().isEmpty());
    }

    @Test
    void missingPrNumberYieldsNull()
    {
        ReviewProject project = project("GITHUB", "https://github.com/acme/demo", "acme", "demo");
        assertNull(ReviewSummaryContentFactory.buildMergeRequestUrl(project, null));
    }

    private static ReviewProject project(String provider, String repositoryUrl, String owner, String name)
    {
        ReviewProject project = new ReviewProject();
        project.setProvider(provider);
        project.setRepositoryUrl(repositoryUrl);
        project.setRepositoryOwner(owner);
        project.setRepositoryName(name);
        return project;
    }
}
