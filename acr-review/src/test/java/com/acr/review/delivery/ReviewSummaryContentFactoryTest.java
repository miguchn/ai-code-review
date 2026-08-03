package com.acr.review.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import com.acr.review.domain.ReviewProject;

/** 总结评论中合并请求 Web 链接的平台路径规则（§7.3）。 */
class ReviewSummaryContentFactoryTest
{
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
