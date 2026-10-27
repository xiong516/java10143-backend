package com.java10143.util;

import cn.hutool.core.util.StrUtil;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** JSON 小工具 */
public class Json {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String to(Object o) {
        try {
            // 注意：o == null 时 Jackson 会返回字符串 "null"，
            // 而 "null" 再反序列化回来是 null（不是空集合）——见下面几个方法里的空值兜底
            return MAPPER.writeValueAsString(o);
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * 反序列化成字符串列表。
     * 注意：Jackson 读 "null" 会返回 null（不是抛异常），所以这里必须再兜一次底，
     * 否则调用方拿到的就是 null，pics.isEmpty() 直接 NPE。
     */
    public static List<String> strList(String json) {
        if (json == null || StrUtil.isBlank(json)) return new ArrayList<>();
        try {
            List<String> list = MAPPER.readValue(json, new TypeReference<List<String>>() {
            });
            return list == null ? new ArrayList<String>() : list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static List<Map<String, Object>> mapList(String json) {
        if (json == null || StrUtil.isBlank(json)) return new ArrayList<>();
        try {
            List<Map<String, Object>> list = MAPPER.readValue(json,
                    new TypeReference<List<Map<String, Object>>>() {
                    });
            return list == null ? new ArrayList<Map<String, Object>>() : list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static Map<String, Object> toMap(String json) {
        if (json == null || StrUtil.isBlank(json)) return Collections.emptyMap();
        try {
            Map<String, Object> map = MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
            return map == null ? Collections.<String, Object>emptyMap() : map;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    public static List<String> parseIds(String raw) {
        if (raw == null || StrUtil.isBlank(raw)) return new ArrayList<>();
        String s = raw.trim();
        List<String> list = new ArrayList<>();
        if (s.startsWith("[")) {
            list.addAll(strList(s));
        } else {
            for (String x : s.split(",")) {
                if (!StrUtil.isBlank(x)) list.add(x.trim().replace("\"", ""));
            }
        }
        return list;
    }
}
