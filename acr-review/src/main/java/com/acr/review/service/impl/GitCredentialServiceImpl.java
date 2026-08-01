package com.acr.review.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.acr.common.exception.ServiceException;
import com.acr.common.utils.SecurityUtils;
import com.acr.common.utils.StringUtils;
import com.acr.review.domain.GitCredential;
import com.acr.review.git.GitConnectionResult;
import com.acr.review.git.GitProvider;
import com.acr.review.mapper.GitCredentialMapper;
import com.acr.review.security.CredentialCryptoService;
import com.acr.review.service.IGitCredentialService;

/** GitHub PAT 凭据管理。 */
@Service
public class GitCredentialServiceImpl implements IGitCredentialService
{
    private static final String PROVIDER = "GITHUB";
    private static final String AUTH_TYPE = "PAT";

    private final GitCredentialMapper credentialMapper;
    private final CredentialCryptoService cryptoService;
    private final GitProvider gitProvider;

    public GitCredentialServiceImpl(GitCredentialMapper credentialMapper,
                                    CredentialCryptoService cryptoService,
                                    GitProvider gitProvider)
    {
        this.credentialMapper = credentialMapper;
        this.cryptoService = cryptoService;
        this.gitProvider = gitProvider;
    }

    @Override
    public GitCredential selectGitCredentialById(Long credentialId)
    {
        GitCredential credential = credentialMapper.selectGitCredentialById(credentialId);
        if (credential == null)
        {
            throw new ServiceException("GitHub 凭据不存在");
        }
        return credential;
    }

    @Override
    public List<GitCredential> selectGitCredentialList(GitCredential credential)
    {
        credential.setProvider(PROVIDER);
        return credentialMapper.selectGitCredentialList(credential);
    }

    @Override
    public int insertGitCredential(GitCredential credential)
    {
        normalize(credential);
        if (StringUtils.isEmpty(credential.getToken()))
        {
            throw new ServiceException("新增 GitHub 凭据时必须输入 Token");
        }
        checkNameUnique(credential);
        credential.setTokenCiphertext(cryptoService.encrypt(credential.getToken().trim()));
        credential.setCreateBy(SecurityUtils.getUsername());
        return credentialMapper.insertGitCredential(credential);
    }

    @Override
    @Transactional
    public int updateGitCredential(GitCredential credential)
    {
        if (credential.getCredentialId() == null)
        {
            throw new ServiceException("凭据 ID 不能为空");
        }
        selectGitCredentialById(credential.getCredentialId());
        normalize(credential);
        checkNameUnique(credential);
        boolean tokenChanged = StringUtils.isNotEmpty(credential.getToken());
        if (tokenChanged)
        {
            credential.setTokenCiphertext(cryptoService.encrypt(credential.getToken().trim()));
        }
        credential.setUpdateBy(SecurityUtils.getUsername());
        int rows = credentialMapper.updateGitCredential(credential);
        if (tokenChanged)
        {
            credentialMapper.resetConnectionCheck(credential.getCredentialId());
        }
        return rows;
    }

    @Override
    @Transactional
    public void deleteGitCredentialByIds(Long[] credentialIds)
    {
        for (Long credentialId : credentialIds)
        {
            GitCredential credential = selectGitCredentialById(credentialId);
            int references = credentialMapper.countProjectsByCredentialId(credentialId);
            if (references > 0)
            {
                throw new ServiceException("凭据“" + credential.getCredentialName() + "”已被 " + references + " 个项目引用，不能删除");
            }
        }
        credentialMapper.deleteGitCredentialByIds(credentialIds);
    }

    @Override
    public GitConnectionResult testConnection(Long credentialId)
    {
        String token = getPlainToken(credentialId, false);
        GitConnectionResult result = gitProvider.testCredential(token);
        GitCredential update = new GitCredential();
        update.setCredentialId(credentialId);
        update.setLastCheckStatus(result.isSuccess() ? "SUCCESS" : "FAILED");
        update.setLastCheckMessage(result.getMessage());
        update.setLastCheckTime(result.getCheckedAt());
        update.setUpdateBy(SecurityUtils.getUsername());
        credentialMapper.updateConnectionCheck(update);
        return result;
    }

    @Override
    public String getPlainToken(Long credentialId, boolean requireEnabled)
    {
        GitCredential credential = credentialMapper.selectGitCredentialSecretById(credentialId);
        if (credential == null)
        {
            throw new ServiceException("GitHub 凭据不存在");
        }
        if (requireEnabled && !"0".equals(credential.getStatus()))
        {
            throw new ServiceException("项目绑定的 GitHub 凭据已停用");
        }
        return cryptoService.decrypt(credential.getTokenCiphertext());
    }

    private void normalize(GitCredential credential)
    {
        credential.setCredentialName(credential.getCredentialName().trim());
        credential.setProvider(PROVIDER);
        credential.setAuthType(AUTH_TYPE);
        if (!"0".equals(credential.getStatus()) && !"1".equals(credential.getStatus()))
        {
            credential.setStatus("0");
        }
    }

    private void checkNameUnique(GitCredential credential)
    {
        if (credentialMapper.selectByProviderAndName(PROVIDER, credential.getCredentialName(), credential.getCredentialId()) != null)
        {
            throw new ServiceException("GitHub 凭据名称已存在");
        }
    }
}
