package com.acr.common.ai;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI Prompt 模板管理
 */
public class AiPromptBuilder
{
    private static final Map<String, String> TEMPLATES = new ConcurrentHashMap<>();

    static
    {
        // 文档解析
        TEMPLATES.put("parse",
            "你是一个专业的 API 文档解析助手。请分析以下接口文档内容，"
            + "提取其中的全部接口信息，以 JSON 格式返回。\n\n"
            + "【重要】只返回纯 JSON 对象，不要包含任何说明文字、前言、后记，不要使用 Markdown 代码块标记（```json 或 ```）。\n\n"
            + "【解析质量要求】\n"
            + "1. 若文档含表格（含「# TABLE」或 Markdown 表格），请按列语义还原：参数名、类型、必填、说明、示例值等；不要丢弃表头与合并单元格对应的说明文字。\n"
            + "2. 对 JSON/XML 请求体、响应体示例，除解析为平铺字段外，可将原始示例完整放入对应字段的 example 或 responses[].example。\n"
            + "3. 嵌套对象、数组元素对象：在 params 或 responses[].bodyFields 中使用 children 数组表达子字段；同时父节点 name 保留对象名，子节点 name 为相对字段名（解析器会展开为点号路径）。\n"
            + "4. 无法确定的字段：required 置为 false，description 可写「待确认」；禁止臆造业务含义。\n"
            + "5. 文档中的接口分组/模块名请写入 tags 数组（如 [\"订单\",\"内部\"]）。\n\n"
            + "返回格式必须是一个 JSON 对象，结构如下：\n"
            + "{\n"
            + "  \"baseUrl\": \"文档中的服务器基础地址（如 https://api.example.com/v1），如果文档中未明确指定则为空字符串\",\n"
            + "  \"apis\": [\n"
            + "    {\n"
            + "      \"path\": \"接口路径（如 /api/user/list）\",\n"
            + "      \"method\": \"HTTP 方法（GET/POST/PUT/DELETE/PATCH）\",\n"
            + "      \"summary\": \"接口摘要\",\n"
            + "      \"description\": \"接口描述\",\n"
            + "      \"operationId\": \"可选，文档中的接口编号或英文标识\",\n"
            + "      \"tags\": [\"可选分组标签\"],\n"
            + "      \"params\": [{\"paramType\":\"query|path|header|body|cookie\",\"name\":\"参数名\",\"dataType\":\"类型\","
            + "\"required\": true 或 false,\"description\":\"说明\",\"example\":\"示例\",\"defaultValue\":\"默认值\","
            + "\"format\":\"如 date-time\",\"enum\":[\"可选枚举\"],"
            + "\"children\":[{\"name\":\"子字段\",\"paramType\":\"body\",\"dataType\":\"string\",\"required\":false,\"description\":\"\"}] }],\n"
            + "      \"responseFields\": [ 与 params 同结构的数组，表示成功响应体字段（可选，与 responses 二选一或并存） ],\n"
            + "      \"responses\": [{\"statusCode\":\"200\",\"description\":\"描述\",\"contentType\":\"application/json\","
            + "\"schemaContent\":\"可选 JSON Schema 片段字符串\",\"example\":\"完整响应 JSON 示例字符串\","
            + "\"bodyFields\": [ 与 params 同结构，表示响应体字段树（可选） ] }]\n"
            + "    }\n"
            + "  ]\n"
            + "}\n\n"
            + "如果文档中包含多个接口，请全部提取。如果某个字段无法确定，可以留空但不要省略字段。\n\n"
            + "文档内容：\n{content}");

        // JSON 修复（二次修复用）
        TEMPLATES.put("repair",
            "你是一个 JSON 修复助手。以下 JSON 内容不完整（可能被截断或缺少闭合括号），"
            + "请修复并返回一个完整的 JSON 对象。\n\n"
            + "【重要】只返回纯 JSON 对象，不要包含任何说明文字，不要使用 Markdown 代码块标记。\n\n"
            + "修复要求：\n"
            + "1. 补齐缺失的闭合括号（花括号和方括号）\n"
            + "2. 如果有截断的接口对象，请补全该对象或将其移除\n"
            + "3. 确保返回的 JSON 结构完整：{\"apis\": [...]} \n"
            + "4. 保持已有接口信息不变，不要修改或删除已有的完整接口\n\n"
            + "需要修复的 JSON 内容：\n{content}");

        // 业务域标注
        TEMPLATES.put("label",
            "你是一个业务分析专家。请根据以下接口的路径和描述，判断它属于哪个业务域。\n\n"
            + "接口路径：{path}\n"
            + "接口方法：{method}\n"
            + "接口描述：{description}\n\n"
            + "请仅返回业务域名称，如：理赔、保单、用户、支付、理赔管理、保单服务、系统管理等。如果无法判断，返回\"未分类\"。");

        // 缺失补全（文档资产：仅补充说明文档文字/示例，不指导改造业务接口或代码）
        TEMPLATES.put("fill",
            "你是企业「接口资产文档」编辑顾问。根据提供的上下文，仅为「接口说明文档」缺失项给出可粘贴进文档的参考表述。\n\n"
            + "【必须遵守】不要建议新增/删除/修改线上接口的真实入参或出参字段，不要建议修改业务代码；不足处请用「建议在文档中补充……」表述。\n\n"
            + "现有信息：{context}\n"
            + "缺失的文档要素键：{missing}\n\n"
            + "请以 JSON 对象返回，key 为缺失键，value 为建议的文档说明文字（字符串）。");

        // 语义推荐
        TEMPLATES.put("recommend",
            "你是一个 API 推荐助手。用户描述了一个需求，请从以下候选接口中推荐最匹配的 {topN} 个接口，并说明推荐理由。\n\n"
            + "用户需求：{query}\n"
            + "候选接口列表：{apis}");

        // 自然语言查询
        TEMPLATES.put("query",
            "你是一个查询条件转换助手。请将用户的自然语言问题转换为结构化的查询条件。\n\n"
            + "用户问题：{question}\n\n"
            + "请以 JSON 格式返回查询条件，包含：keyword（关键词）、method（HTTP方法）、businessDomain（业务域）、minCompleteness（最低完整度）等字段。");
    }

    /**
     * 获取模板并替换变量
     */
    public static String build(String taskName, Map<String, String> variables)
    {
        String template = TEMPLATES.get(taskName);
        if (template == null)
        {
            throw new IllegalArgumentException("未知的 Prompt 模板: " + taskName);
        }
        String result = template;
        if (variables != null)
        {
            for (Map.Entry<String, String> entry : variables.entrySet())
            {
                result = result.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return result;
    }
}
