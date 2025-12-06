package com.ai.aiscriptmurde.model;

public class SearchResponse {
    // 1. 请求是否成功 (如果行动力不足，这里可能是 false)
    public boolean success;

    // 2. 搜证结果的描述文本
    // 例如: "你在床底下发现了一个空药瓶。" 或 "这里打扫得很干净，什么都没有。"
    public String message;

    // 3. 获得的新物品名称 (对应物品栏)
    // 如果没有搜到新物品，这个字段可能是 null 或者空字符串
    public String newItem;

    // 4. 搜证后的剩余行动力 (AP)
    // 前端收到这个值后，更新 UI 上的 "AP: 2"
    public int remainingAp;
}