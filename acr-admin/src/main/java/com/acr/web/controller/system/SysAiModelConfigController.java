package com.acr.web.controller.system;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.acr.common.annotation.Log;
import com.acr.common.core.controller.BaseController;
import com.acr.common.core.domain.AjaxResult;
import com.acr.common.core.page.TableDataInfo;
import com.acr.common.enums.BusinessType;
import com.acr.common.enums.LlmProviderCode;
import com.acr.system.domain.SysAiModelConfig;
import com.acr.system.service.ISysAiModelConfigService;

/**
 * 大模型配置 信息操作处理
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

    @PreAuthorize("@ss.hasPlatformPermi('system:aimodelconfig:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysAiModelConfig sysAiModelConfig)
    {
        startPage();
        List<SysAiModelConfig> list = aiModelConfigService.selectSysAiModelConfigList(sysAiModelConfig);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPlatformPermi('system:aimodelconfig:query')")
    @GetMapping("/providers")
    public AjaxResult providers()
    {
        List<Map<String, Object>> providers = Arrays.stream(LlmProviderCode.values())
            .map(code -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("code", code.getCode());
                item.put("label", code.getLabel());
                item.put("domestic", code.isDomestic());
                return item;
            })
            .collect(Collectors.toList());
        return success(providers);
    }

    @PreAuthorize("@ss.hasPlatformPermi('system:aimodelconfig:query')")
    @GetMapping(value = "/{modelId}")
    public AjaxResult getInfo(@PathVariable Long modelId)
    {
        return success(aiModelConfigService.selectSysAiModelConfigById(modelId));
    }

    @PreAuthorize("@ss.hasPlatformPermi('system:aimodelconfig:add')")
    @Log(title = "大模型配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysAiModelConfig sysAiModelConfig)
    {
        sysAiModelConfig.setCreateBy(getUsername());
        return toAjax(aiModelConfigService.insertSysAiModelConfig(sysAiModelConfig));
    }

    @PreAuthorize("@ss.hasPlatformPermi('system:aimodelconfig:edit')")
    @Log(title = "大模型配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysAiModelConfig sysAiModelConfig)
    {
        sysAiModelConfig.setUpdateBy(getUsername());
        return toAjax(aiModelConfigService.updateSysAiModelConfig(sysAiModelConfig));
    }

    @PreAuthorize("@ss.hasPlatformPermi('system:aimodelconfig:remove')")
    @Log(title = "大模型配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{modelIds}")
    public AjaxResult remove(@PathVariable Long[] modelIds)
    {
        aiModelConfigService.deleteSysAiModelConfigByIds(modelIds);
        return success();
    }

    @PreAuthorize("@ss.hasPlatformPermi('system:aimodelconfig:edit')")
    @PutMapping("/{modelId}/enable")
    public AjaxResult enable(@PathVariable Long modelId, @RequestParam String enabled)
    {
        return toAjax(aiModelConfigService.enableModel(modelId, enabled));
    }

    @PreAuthorize("@ss.hasPlatformPermi('system:aimodelconfig:edit')")
    @PutMapping("/{modelId}/default")
    public AjaxResult setDefault(@PathVariable Long modelId)
    {
        return toAjax(aiModelConfigService.setDefaultModel(modelId));
    }

    @PreAuthorize("@ss.hasAnyPlatformPermi('system:aimodelconfig:add,system:aimodelconfig:edit')")
    @PostMapping("/test")
    public AjaxResult testConnection(@RequestBody SysAiModelConfig sysAiModelConfig)
    {
        return success(aiModelConfigService.testConnection(sysAiModelConfig));
    }

    @PreAuthorize("@ss.hasAnyPlatformPermi('system:aimodelconfig:add,system:aimodelconfig:edit')")
    @PostMapping("/test-call")
    public AjaxResult testModelCall(@RequestBody SysAiModelConfig sysAiModelConfig)
    {
        return success(aiModelConfigService.testModelCall(sysAiModelConfig));
    }
}
