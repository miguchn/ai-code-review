package com.acr.web.controller.review;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.acr.common.annotation.Log;
import com.acr.common.core.controller.BaseController;
import com.acr.common.core.domain.AjaxResult;
import com.acr.common.core.page.TableDataInfo;
import com.acr.common.enums.BusinessType;
import com.acr.review.domain.GitCredential;
import com.acr.review.service.IGitCredentialService;

/** GitHub 访问凭据 REST 接入。 */
@RestController
@RequestMapping("/review/credential")
public class GitCredentialController extends BaseController
{
    private final IGitCredentialService credentialService;

    public GitCredentialController(IGitCredentialService credentialService)
    {
        this.credentialService = credentialService;
    }

    @PreAuthorize("@ss.hasPermi('review:credential:list')")
    @GetMapping("/list")
    public TableDataInfo list(GitCredential credential)
    {
        startPage();
        List<GitCredential> list = credentialService.selectGitCredentialList(credential);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('review:credential:query')")
    @GetMapping("/{credentialId}")
    public AjaxResult getInfo(@PathVariable Long credentialId)
    {
        return success(credentialService.selectGitCredentialById(credentialId));
    }

    @PreAuthorize("@ss.hasPermi('review:credential:add')")
    @Log(title = "GitHub访问凭据", businessType = BusinessType.INSERT,
         excludeParamNames = { "token", "tokenCiphertext" }, isSaveResponseData = false)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody GitCredential credential)
    {
        return toAjax(credentialService.insertGitCredential(credential));
    }

    @PreAuthorize("@ss.hasPermi('review:credential:edit')")
    @Log(title = "GitHub访问凭据", businessType = BusinessType.UPDATE,
         excludeParamNames = { "token", "tokenCiphertext" }, isSaveResponseData = false)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody GitCredential credential)
    {
        return toAjax(credentialService.updateGitCredential(credential));
    }

    @PreAuthorize("@ss.hasPermi('review:credential:remove')")
    @Log(title = "GitHub访问凭据", businessType = BusinessType.DELETE)
    @DeleteMapping("/{credentialIds}")
    public AjaxResult remove(@PathVariable Long[] credentialIds)
    {
        credentialService.deleteGitCredentialByIds(credentialIds);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('review:credential:test')")
    @Log(title = "GitHub凭据连接测试", isSaveResponseData = false)
    @PostMapping("/{credentialId}/test")
    public AjaxResult testConnection(@PathVariable Long credentialId)
    {
        return success(credentialService.testConnection(credentialId));
    }
}
