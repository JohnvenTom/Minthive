package com.minthive.common;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class TagMappingConstants {

    private TagMappingConstants() {}

    public static final Map<String, List<String>> KEYWORD_MAP = Map.ofEntries(
        Map.entry("gaming", List.of("游戏", "电竞", "主机", "PC", "手游", "Steam", "PS5", "Xbox", "Switch", "原神", "LOL", "吃鸡")),
        Map.entry("movie", List.of("电影", "影视", "电视剧", "动漫", "番剧", "追剧", "影评", "导演", "剧本")),
        Map.entry("outdoor", List.of("户外", "露营", "徒步", "登山", "骑行", "钓鱼", "探险", "野餐", "自驾")),
        Map.entry("reading", List.of("读书", "阅读", "小说", "文学", "书店", "书单", "读后感", "经典")),
        Map.entry("photography", List.of("摄影", "拍照", "相机", "修图", "风光", "人像", "街拍", "富士", "索尼", "佳能")),
        Map.entry("anime", List.of("二次元", "动漫", "cos", "手办", "漫展", "日漫", "国漫", "番剧")),
        Map.entry("music", List.of("音乐", "唱歌", "乐器", "吉他", "钢琴", "乐队", "说唱", "嘻哈", "古典", "民谣")),
        Map.entry("food", List.of("美食", "做饭", "烘焙", "探店", "吃货", "菜谱", "甜品", "火锅", "烧烤")),
        Map.entry("travel", List.of("旅行", "旅游", "机票", "酒店", "攻略", "背包客", "打卡", "景点")),
        Map.entry("craft", List.of("手工", "DIY", "手作", "编织", "木工", "陶艺", "拼图", "乐高")),
        Map.entry("sports", List.of("运动", "健身", "跑步", "篮球", "足球", "羽毛球", "游泳", "瑜伽", "撸铁")),
        Map.entry("tech", List.of("科技", "数码", "编程", "AI", "人工智能", "手机", "电脑", "互联网", "软件", "硬件")),
        Map.entry("pet", List.of("宠物", "猫", "狗", "猫咪", "狗狗", "铲屎官", "养宠", "萌宠")),
        Map.entry("fashion", List.of("时尚", "穿搭", "潮牌", "美妆", "护肤", "发型", "配饰", "奢侈品")),
        Map.entry("art", List.of("艺术", "绘画", "油画", "国画", "书法", "雕塑", "展览", "美术馆")),
        Map.entry("dance", List.of("舞蹈", "跳舞", "街舞", "芭蕾", "拉丁", "Kpop", "编舞")),
        Map.entry("finance", List.of("理财", "股票", "基金", "投资", "保险", "经济", "财经", "区块链", "Web3")),
        Map.entry("health", List.of("养生", "健康", "中医", "保健", "睡眠", "冥想", "正念", "养生茶")),
        Map.entry("education", List.of("教育", "学习", "考试", "考研", "留学", "英语", "课程", "知识")),
        Map.entry("design", List.of("设计", "UI", "UX", "平面", "插画", "3D", "建筑", "室内")),
        Map.entry("writing", List.of("写作", "文案", "小说", "诗歌", "投稿", "出版", "自媒体")),
        Map.entry("astrology", List.of("星座", "占星", "塔罗", "运势", "性格", "十二星座")),
        Map.entry("cars", List.of("汽车", "改装", "赛车", "新能源", "特斯拉", "驾驶", "自驾游")),
        Map.entry("digital", List.of("数码", "手机", "电脑", "耳机", "智能家居", "平板", "笔记本", "评测"))
    );

    /**
     * 所有兴趣标签的英文 key 集合
     */
    public static final Set<String> ALL_KEYS = KEYWORD_MAP.keySet();

    /**
     * 将中文标签列表映射到命中的兴趣标签 key
     *
     * @param tags 帖子的话题标签列表（中文）
     * @return 命中的兴趣标签 key 集合
     */
    public static Set<String> matchTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Set.of();
        }
        return tags.stream()
            .flatMap(tag -> KEYWORD_MAP.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(kw -> tag.contains(kw)))
                .map(Map.Entry::getKey))
            .collect(Collectors.toSet());
    }

    /**
     * 将逗号分隔的标签字符串解析并匹配兴趣标签
     *
     * @param tagStr 逗号分隔的标签字符串
     * @return 命中的兴趣标签 key 集合
     */
    public static Set<String> matchTagsFromString(String tagStr) {
        if (tagStr == null || tagStr.isBlank()) {
            return Set.of();
        }
        List<String> tags = List.of(tagStr.split(","));
        return matchTags(tags.stream().map(String::trim).collect(Collectors.toList()));
    }
}
