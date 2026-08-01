package com.acr.review.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.acr.review.domain.GitCredential;

/** Git 凭据数据访问。 */
public interface GitCredentialMapper
{
    GitCredential selectGitCredentialById(Long credentialId);

    GitCredential selectGitCredentialSecretById(Long credentialId);

    List<GitCredential> selectGitCredentialList(GitCredential credential);

    GitCredential selectByProviderAndName(@Param("provider") String provider,
                                          @Param("credentialName") String credentialName,
                                          @Param("excludeCredentialId") Long excludeCredentialId);

    int insertGitCredential(GitCredential credential);

    int updateGitCredential(GitCredential credential);

    int updateConnectionCheck(GitCredential credential);

    int resetConnectionCheck(Long credentialId);

    int deleteGitCredentialByIds(Long[] credentialIds);

    int countProjectsByCredentialId(Long credentialId);
}
