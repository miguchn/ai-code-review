package com.acr.review.insight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.acr.review.git.GitPushCommit;
import com.acr.review.git.GitPushEvent;
import com.acr.review.mapper.ReviewCommitFactMapper;

@ExtendWith(MockitoExtension.class)
class ReviewCommitFactIngestServiceTest
{
    @Mock
    private ReviewCommitFactMapper commitFactMapper;

    private ReviewCommitFactIngestService service;

    @BeforeEach
    void setUp()
    {
        service = new ReviewCommitFactIngestService(commitFactMapper);
    }

    @Test
    void ingestUsesInsertIgnoreBatchAndIsIdempotentAcrossCalls()
    {
        GitPushEvent event = pushWithCommits(List.of(
            new GitPushCommit("sha1", "Alice", "alice@example.com", new Date(), "msg1"),
            new GitPushCommit("sha2", "Bob", "bob@example.com", new Date(), "msg2")));
        when(commitFactMapper.insertIgnoreBatch(anyList())).thenReturn(2, 0);

        assertEquals(2, service.ingestFromPush(9L, 100L, event));
        assertEquals(0, service.ingestFromPush(9L, 100L, event));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ReviewCommitFact>> captor = ArgumentCaptor.forClass(List.class);
        verify(commitFactMapper, times(2)).insertIgnoreBatch(captor.capture());
        List<ReviewCommitFact> first = captor.getAllValues().get(0);
        assertEquals(2, first.size());
        assertEquals(9L, first.get(0).getProjectId());
        assertEquals("sha1", first.get(0).getCommitSha());
        assertEquals(100L, first.get(0).getSourceEventId());
        assertEquals("alice@example.com", first.get(0).getAuthorEmail());
    }

    @Test
    void ingestFailureDoesNotThrow()
    {
        GitPushEvent event = pushWithCommits(List.of(
            new GitPushCommit("sha1", "Alice", "a@x.com", new Date(), "msg")));
        when(commitFactMapper.insertIgnoreBatch(anyList())).thenThrow(new RuntimeException("db down"));

        assertEquals(0, service.ingestFromPush(1L, 2L, event));
    }

    @Test
    void emptyCommitsReturnsZeroWithoutMapperCall()
    {
        GitPushEvent event = pushWithCommits(List.of());
        assertEquals(0, service.ingestFromPush(1L, 2L, event));
        verify(commitFactMapper, times(0)).insertIgnoreBatch(anyList());
    }

    private static GitPushEvent pushWithCommits(List<GitPushCommit> commits)
    {
        return new GitPushEvent(
            "d-1", "o", "r", "o/r", "main",
            "aaaabbbbccccddddeeeeffff0000111122223333",
            "ffffeeeeddddccccbbbbaaaa3333222211110000",
            "alice", commits.size(), "head", false, false, commits);
    }
}
