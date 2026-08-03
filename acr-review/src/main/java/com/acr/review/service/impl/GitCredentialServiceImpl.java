package com.acr.review.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.acr.common.exception.ServiceException;
import com.acr.common.utils.SecurityUtils;
import com.acr.common.utils.StringUtils;
import com.acr.review.domain.GitCredential;
import com.acr.review.git.GitAccessContext;
import com.acr.review.git.GitAdapterRegistry;
import com.acr.review.git.GitConnectionResult;
import com.acr.review.git.GitProvider;
import com.acr.review.git.GitProviderCodes;
import com.acr.review.mapper.GitCredentialMapper;
import com.acr.review.security.CredentialCryptoService;
import com.acr.review.service.IGitCredentialService;

/** Git 访问凭据管理（多平台）。 */
@Service
public class GitCredentialServiceImpl implements IGitCredentialService
{
    private static final String AUTH_TYPE = "PAT";

    private final GitCredentialMapper credentialMapper;
    private final CredentialCryptoService cryptoService;
    private final GitAdapterRegistry adapterRegistry;

    public GitCredentialServiceImpl(GitCredentialMapper credentialMapper,
                                    CredentialCryptoService cryptoService,
                                    GitAdapterRegistry adapterRegistry)
    {
        this.credentialMapper = credentialMapper;
        this.cryptoService = cryptoService;
        this.adapterRegistry = adapterRegistry;
    }

    @Override
    public GitCredential selectGitCredentialById(Long credentialId)
    {
        GitCredential credential = credentialMapper.selectGitCredentialById(credentialId);
        if (credential == null)
        {
            throw new ServiceException("Git 凭据不存在");
        }
        return credential;
    }

    @Override
    public List<GitCredential> selectGitCredentialList(GitCredential credential)
    {
        return credentialMapper.selectGitCredentialList(credential);
    }

    @Override
    public int insertGitCredential(GitCredential credential)
    {
        normalize(credential);
        if (StringUtils.isEmpty(credential.getToken()))
        {
            throw new ServiceException("新增凭据时必须输入 Token");
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
        GitCredential credential = selectGitCredentialById(credentialId);
        String token = getPlainToken(credentialId, false);
        String provider = credential.getProvider();
        GitProvider gitProvider = adapterRegistry.requireProvider(provider);
        GitConnectionResult result = gitProvider.testCredential(
            GitAccessContext.of(token, resolveServerUrl(provider, credential.getServerUrl())));
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
            throw new ServiceException("Git 凭据不存在");
        }
        if (requireEnabled && !"0".equals(credential.getStatus()))
        {
            throw new ServiceException("项目绑定的 Git 凭据已停用");
        }
        return cryptoService.decrypt(credential.getTokenCiphertext());
    }

    private void normalize(GitCredential credential)
    {
        credential.setCredentialName(credential.getCredentialName().trim());
        if (StringUtils.isEmpty(credential.getProvider()))
        {
            throw new ServiceException("Git Provider 不能为空");
        }
        String provider = credential.getProvider().trim().toUpperCase(Locale.ROOT);
        if (!GitProviderCodes.isSupported(provider))
        {
            throw new ServiceException("暂不支持的 Git Provider：" + provider);
        }
        credential.setProvider(provider);
        credential.setAuthType(AUTH_TYPE);
        credential.setServerUrl(normalizeStoredServerUrl(provider, credential.getServerUrl()));
        if (!"0".equals(credential.getStatus()) && !"1".equals(credential.getStatus()))
        {
            credential.setStatus("0");
        }
    }

    private void checkNameUnique(GitCredential credential)
    {
        if (credentialMapper.selectByProviderAndName(credential.getProvider(), credential.getCredentialName(),
            credential.getCredentialId()) != null)
        {
            throw new ServiceException("同平台下凭据名称已存在");
        }
    }

    static String normalizeStoredServerUrl(String provider, String serverUrl)
    {
        if (GitProviderCodes.requiresServerUrl(provider))
        {
            if (StringUtils.isEmpty(serverUrl))
            {
                throw new ServiceException("GitLab/Gitea 凭据必须填写服务地址");
            }
            return GitAccessContext.normalizeServerUrl(serverUrl.trim());
        }
        if (GitProviderCodes.forbidsServerUrl(provider) && StringUtils.isNotEmpty(serverUrl))
        {
            throw new ServiceException("GitHub/Gitee 凭据不需要填写服务地址");
        }
        return null;
    }

    static String resolveServerUrl(String provider, String serverUrl)
    {
        if (GitProviderCodes.requiresServerUrl(provider))
        {
            if (StringUtils.isEmpty(serverUrl))
            {
                throw new ServiceException("凭据缺少服务地址，请重新保存");
            }
            return GitAccessContext.normalizeServerUrl(serverUrl);
        }
        String defaultUrl = GitProviderCodes.defaultServerUrl(provider);
        if (defaultUrl == null)
        {
            throw new ServiceException("无法解析平台默认服务地址：" + provider);
        }
        return defaultUrl;
    }
}
