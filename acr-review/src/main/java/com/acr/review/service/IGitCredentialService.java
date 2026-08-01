package com.acr.review.service;

import java.util.List;
import com.acr.review.domain.GitCredential;
import com.acr.review.git.GitConnectionResult;

/** Git 凭据用例。 */
public interface IGitCredentialService
{
    GitCredential selectGitCredentialById(Long credentialId);

    List<GitCredential> selectGitCredentialList(GitCredential credential);

    int insertGitCredential(GitCredential credential);

    int updateGitCredential(GitCredential credential);

    void deleteGitCredentialByIds(Long[] credentialIds);

    GitConnectionResult testConnection(Long credentialId);

    String getPlainToken(Long credentialId, boolean requireEnabled);
}
