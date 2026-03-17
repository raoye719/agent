package com.yupi.yuaiagent.tools;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 网页搜索工具
 */
public class WebSearchTool {

    // SearchAPI 的搜索接口地址
    private static final String SEARCH_API_URL = "https://www.searchapi.io/api/v1/search";

    private final String apiKey;

    public WebSearchTool(String apiKey) {
        this.apiKey = apiKey;
    }

    @Tool(description = "Search for information from Baidu Search Engine")
    public String searchWeb(
            @ToolParam(description = "Search query keyword") String query) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("q", query);
        paramMap.put("api_key", apiKey);
        paramMap.put("engine", "baidu");
        try {
            String response = HttpUtil.get(SEARCH_API_URL, paramMap);
            JSONObject jsonObject = JSONUtil.parseObj(response);
            // 优先取 organic_results，没有则尝试 answer_box 或直接返回原始响应摘要
            JSONArray organicResults = jsonObject.getJSONArray("organic_results");
            if (organicResults == null || organicResults.isEmpty()) {
                // 尝试取 answer_box
                JSONObject answerBox = jsonObject.getJSONObject("answer_box");
                if (answerBox != null) {
                    return answerBox.toString();
                }
                // 返回错误信息，告知 AI Key 可能无效
                String errorMsg = jsonObject.getStr("error", "");
                if (!errorMsg.isEmpty()) {
                    return "搜索失败（API错误）：" + errorMsg + "。请直接根据已有知识回答用户问题并调用 terminate 结束。";
                }
                return "搜索未返回结果，请直接根据已有知识回答用户问题并调用 terminate 结束。";
            }
            int size = Math.min(5, organicResults.size());
            List<Object> objects = organicResults.subList(0, size);
            String result = objects.stream().map(obj -> {
                JSONObject tmpJSONObject = (JSONObject) obj;
                // 只保留 title、snippet、link，不要完整 JSON
                JSONObject slim = new JSONObject();
                slim.set("title", tmpJSONObject.getStr("title", ""));
                slim.set("snippet", tmpJSONObject.getStr("snippet", ""));
                slim.set("link", tmpJSONObject.getStr("link", ""));
                return slim.toString();
            }).collect(Collectors.joining(","));
            return result;
        } catch (Exception e) {
            return "搜索出错：" + e.getMessage() + "。请直接根据已有知识回答用户问题并调用 terminate 结束。";
        }
    }
}
