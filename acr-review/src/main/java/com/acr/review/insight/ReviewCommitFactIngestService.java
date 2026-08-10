package com.acr.review.insight;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.acr.common.utils.StringUtils;
import com.acr.review.git.GitPushCommit;
import com.acr.review.git.GitPushEvent;
import com.acr.review.mapper.ReviewCommitFactMapper;

/** 推送受理后抽取 commits → review_commit_fact（INSERT IGNORE 幂等）。 */
@Service
public class ReviewCommitFactIngestService
{
    private static final Logger log = LoggerFactory.getLogger(ReviewCommitFactIngestService.class);

    private final ReviewCommitFactMapper commitFactMapper;

    public ReviewCommitFactIngestService(ReviewCommitFactMapper commitFactMapper)
    {
        this.commitFactMapper = commitFactMapper;
    }

    /**
     * 抽取失败仅告警，不抛出。返回尝试写入条数（含 IGNORE 跳过）。
     */
    public int ingestFromPush(Long projectId, Long eventId, GitPushEvent pushEvent)
    {
        if (projectId == null || pushEvent == null || pushEvent.commits() == null || pushEvent.commits().isEmpty())
        {
            return 0;
        }
        try
        {
            List<ReviewCommitFact> rows = new ArrayList<>();
            Date fallbackTime = new Date();
            for (GitPushCommit commit : pushEvent.commits())
            {
                if (commit == null || StringUtils.isEmpty(commit.sha()))
                {
                    continue;
                }
                ReviewCommitFact fact = new ReviewCommitFact();
                fact.setProjectId(projectId);
                fact.setCommitSha(commit.sha());
                fact.setCommitTime(commit.timestamp() != null ? commit.timestamp() : fallbackTime);
                fact.setAuthorName(commit.authorName());
                fact.setAuthorEmail(commit.authorEmail());
                fact.setMessageFirstLine(commit.messageFirstLine());
                fact.setSourceEventId(eventId);
                rows.add(fact);
            }
            if (rows.isEmpty())
            {
                return 0;
            }
            return commitFactMapper.insertIgnoreBatch(rows);
        }
        catch (RuntimeException ex)
        {
            log.warn("抽取推送提交事实失败，不影响受理主链路。projectId={}, eventId={}, reason={}",
                projectId, eventId, ex.getMessage());
            return 0;
        }
    }
}
