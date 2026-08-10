package com.acr.review.git;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class GitCommandRunnerTest
{
    @Test
    void classifiesOnlyTransientRemoteFailuresAsRetryable()
    {
        assertTrue(new GitCommandRunner.GitCommandResult(
            128, 10, "fatal: unable to access: Could not resolve host: git.example.com", false)
            .transientDependencyFailure());
        assertTrue(new GitCommandRunner.GitCommandResult(
            128, 10, "fatal: the remote end hung up unexpectedly", false)
            .transientDependencyFailure());
        assertFalse(new GitCommandRunner.GitCommandResult(
            128, 10, "fatal: Not a valid object name deadbeef", false)
            .transientDependencyFailure());
        assertFalse(new GitCommandRunner.GitCommandResult(
            128, 10, "remote: HTTP Basic: Access denied", false)
            .transientDependencyFailure());
    }
}
