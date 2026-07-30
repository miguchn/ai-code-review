package com.acr.web.controller.system;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.acr.common.annotation.Log;
import com.acr.common.core.controller.BaseController;
import com.acr.common.core.domain.AjaxResult;
import com.acr.common.core.page.TableDataInfo;
import com.acr.common.enums.BusinessType;
import com.acr.system.domain.SysAiModelConfig;
import com.acr.system.service.ISysAiModelConfigService;

/**
 * AI 大模型配置 信息操作处理
 */
@RestController
@RequestMapping("/system/aimodelconfig")
public class SysAiModelConfigController extends BaseController
{
    private final ISysAiModelConfigService aiModelConfigService;

    public SysAiModelConfigController(ISysAiModelConfigService aiModelConfigService)
    {
        this.aiModelConfigService = aiModelConfigService;
    }

    /** 获取 AI 大模型配置列表 */
    @PreAuthorize("@ss.hasPermi('system:aimodelconfig:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysAiModelConfig sysAiModelConfig)
    {
        startPage();
        List<SysAiModelConfig> list = aiModelConfigService.selectSysAiModelConfigList(sysAiModelConfig);
        return getDataTable(list);
    }

    /** 获取 AI 大模型配置详细信息 */
    @PreAuthorize("@ss.hasPermi('system:aimodelconfig:query')")
    @GetMapping(value = "/{modelId}")
    public AjaxResult getInfo(@PathVariable Long modelId)
    {
        return success(aiModelConfigService.selectSysAiModelConfigById(modelId));
    }

    /** 新增 AI 大模型配置 */
    @PreAuthorize("@ss.hasPermi('system:aimodelconfig:add')")
    @Log(title = "AI大模型配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysAiModelConfig sysAiModelConfig)
    {
        sysAiModelConfig.setCreateBy(getUsername());
        return toAjax(aiModelConfigService.insertSysAiModelConfig(sysAiModelConfig));
    }

    /** 修改 AI 大模型配置 */
    @PreAuthorize("@ss.hasPermi('system:aimodelconfig:edit')")
    @Log(title = "AI大模型配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysAiModelConfig sysAiModelConfig)
    {
        sysAiModelConfig.setUpdateBy(getUsername());
        return toAjax(aiModelConfigService.updateSysAiModelConfig(sysAiModelConfig));
    }

    /** 删除 AI 大模型配置 */
    @PreAuthorize("@ss.hasPermi('system:aimodelconfig:remove')")
    @Log(title = "AI大模型配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{modelIds}")
    public AjaxResult remove(@PathVariable Long[] modelIds)
    {
        aiModelConfigService.deleteSysAiModelConfigByIds(modelIds);
        return success();
    }

    /** 启用/禁用模型 */
    @PreAuthorize("@ss.hasPermi('system:aimodelconfig:edit')")
    @PutMapping("/{modelId}/enable")
    public AjaxResult enable(@PathVariable Long modelId, @RequestParam String enabled)
    {
        return toAjax(aiModelConfigService.enableModel(modelId, enabled));
    }

    /** 设为默认模型 */
    @PreAuthorize("@ss.hasPermi('system:aimodelconfig:edit')")
    @PutMapping("/{modelId}/default")
    public AjaxResult setDefault(@PathVariable Long modelId)
    {
        return toAjax(aiModelConfigService.setDefaultModel(modelId));
    }

    /** 测试大模型连接 */
    @PreAuthorize("@ss.hasPermi('system:aimodelconfig:query')")
    @PostMapping("/test")
    public AjaxResult testConnection(@RequestBody SysAiModelConfig sysAiModelConfig)
    {
        String result = aiModelConfigService.testConnection(sysAiModelConfig);
        return success(result);
    }
}
