package com.java10143.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.java10143.common.BizException;
import com.java10143.entity.Spu;
import com.java10143.entity.Video;
import com.java10143.mapper.SpuMapper;
import com.java10143.mapper.VideoMapper;
import com.java10143.util.Json;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 关键词生成商品短视频（AI 视频合成）
 *   POST /admin/video/generate
 *   body: {"keyword":"口红", "spuId":1001(可选), "style":"seeds|unbox|flash|fresh"(可选), "duration":10(可选)}
 *
 * 流程：
 *   1. 选商品（spuId 直选 or 关键词 like），拿 AI 商品图 media/goods/g{id}.png 做封面
 *   2. 调 Python AI 服务 /ai/genvideo，ffmpeg 按风格运镜（慢推/横移/快闪/淡出）+ 烧录字幕
 *   3. 生成结果直接入库 video 表，信息流立刻能看到
 */
@Service
@RequiredArgsConstructor
public class VideoGenService {

    private final SpuMapper spuMapper;
    private final VideoMapper videoMapper;

    @Value("${java10143.ai.url:http://127.0.0.1:8010}")
    private String aiUrl;

    /** 服务自身地址：后台任务无请求上下文，用它把 /img/xxx 拼成绝对 URL 交给 Python 下载
     *  默认走局域网 IP，手机/模拟器也能下载；换网络改 application.yml 或环境变量 */
    @Value("${java10143.public-url:http://192.168.100.200:8080}")
    private String publicUrl;

    /** 合成视频比较慢，读超时给足 180 秒（15 秒时长 + 字幕渲染更久） */
    private static final int GEN_READ_TIMEOUT_MS = 180000;

    /** 风格预设（与 ai-service/server.py 的 GEN_STYLES 一一对应） */
    private static final Map<String, String> STYLE_NAMES = new LinkedHashMap<>();
    static {
        STYLE_NAMES.put("seeds", "种草慢推");
        STYLE_NAMES.put("unbox", "开箱横移");
        STYLE_NAMES.put("flash", "快闪节奏");
        STYLE_NAMES.put("fresh", "清新淡出");
    }

    /**
     * 生成 AI 商品短视频。
     * body: keyword(必填) / spuId(可选，直接指定商品) / style(可选，默认 seeds) / duration(可选 5~20，默认 10)
     */
    public Map<String, Object> generate(Map<String, Object> body) {
        Map<String, Object> req = body == null ? new LinkedHashMap<>() : body;
        String keyword = req.get("keyword") == null ? "" : String.valueOf(req.get("keyword")).trim();
        if (keyword.isEmpty()) {
            throw new BizException("请输入关键词或选择商品，例如：口红 / 牛奶 / 空气炸锅");
        }

        // 风格与时长（越界兜底）
        String style = req.get("style") == null ? "seeds" : String.valueOf(req.get("style")).trim();
        if (!STYLE_NAMES.containsKey(style)) style = "seeds";
        String styleName = STYLE_NAMES.get(style);
        int duration = 10;
        try {
            duration = Integer.parseInt(String.valueOf(req.get("duration")).trim());
        } catch (Exception ignored) {
        }
        duration = Math.max(5, Math.min(20, duration));

        // 1. 找商品：优先直接指定 spuId，否则按关键词模糊搜（销量最高的第一个）
        Spu spu;
        Long spuId = null;
        try {
            Object o = req.get("spuId");
            if (o != null && !String.valueOf(o).trim().isEmpty() && !"null".equalsIgnoreCase(String.valueOf(o))) {
                spuId = Long.valueOf(String.valueOf(o).trim());
            }
        } catch (NumberFormatException ignored) {
        }
        if (spuId != null) {
            spu = spuMapper.selectById(spuId);
        } else {
            spu = spuMapper.selectOne(Wrappers.<Spu>lambdaQuery()
                    .like(Spu::getName, keyword)
                    .orderByDesc(Spu::getSalesCount)
                    .last("limit 1"));
        }

        // 2. 封面：优先 AI 商品图，没有就退回主图
        List<String> images = new ArrayList<>();
        String coverPath;
        if (spu != null) {
            File ai = new File("media" + File.separator + "goods" + File.separator + "g" + spu.getId() + ".png");
            if (ai.exists()) {
                images.add("/media/goods/g" + spu.getId() + ".png");
            }
            List<String> pics = Json.strList(spu.getMainPictures());
            if (images.isEmpty() && pics != null && !pics.isEmpty()) {
                images.add(pics.get(0));
            }
            coverPath = images.isEmpty() ? null : images.get(0);
        } else {
            coverPath = null;
        }

        // 3. 调 Python 合成（images 必须是 Python 能直接 http 下载的绝对地址，
        //    相对路径交给它只会 404 → 报「没有可用的封面图」）
        List<String> absImages = new ArrayList<>();
        for (String p : images) {
            if (p == null || p.trim().isEmpty()) continue;
            String s = p.trim();
            if (s.startsWith("http://") || s.startsWith("https://")) absImages.add(s);
            else absImages.add(publicUrl + (s.startsWith("/") ? s : "/" + s));
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("keyword", keyword);
        payload.put("images", absImages);
        payload.put("style", style);
        payload.put("duration", duration);
        System.out.println("[genvideo] POST " + aiUrl + "/ai/genvideo style=" + style
                + " duration=" + duration + " images=" + absImages);
        Map<String, Object> remote = callPython(aiUrl + "/ai/genvideo", Json.to(payload));
        if (remote == null) {
            throw new BizException("AI 视频服务（8010）没有响应，请先启动 ai-service/server.py");
        }
        String file = String.valueOf(remote.get("file"));
        try {
            Object d = remote.get("duration");
            if (d != null) duration = (int) Double.parseDouble(String.valueOf(d));
        } catch (Exception ignored) {
        }
        Object captionsObj = remote.get("captions");

        // 4. 入库 video 表（sort 排到最后，管理员可在后台置顶）
        Video v = new Video();
        v.setSpuId(spu == null ? null : spu.getId());
        v.setTitle((spu == null ? keyword : spu.getName()) + " · AI 宣传视频");
        v.setDescription("由关键词「" + keyword + "」AI 生成 · " + styleName + " · " + duration + " 秒");
        v.setUrl("/media/video/" + file);
        v.setCover(coverPath == null ? "/img/p/p1001.svg" : coverPath);
        v.setAuthorName("AI 视频助手");
        v.setAuthorAvatar("/img/p/p1001.svg");
        v.setTags("AI生成," + styleName);
        v.setDuration(duration);
        v.setSort(maxSort() + 1);
        v.setStatus(1);
        videoMapper.insert(v);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("videoId", String.valueOf(v.getId()));
        res.put("title", v.getTitle());
        res.put("url", v.getUrl());
        res.put("duration", duration);
        res.put("goodsName", spu == null ? null : spu.getName());
        res.put("style", style);
        res.put("styleName", styleName);
        res.put("captions", captionsObj);
        return res;
    }

    private int maxSort() {
        Video top = videoMapper.selectOne(Wrappers.<Video>lambdaQuery()
                .orderByDesc(Video::getSort).last("limit 1"));
        return top == null || top.getSort() == null ? 0 : top.getSort();
    }

    /** 极简 HTTP POST（同 AiService，返回解析后的 result；非 200 返回 null） */
    @SuppressWarnings("unchecked")
    private Map<String, Object> callPython(String url, String json) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(GEN_READ_TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
            if (conn.getResponseCode() != 200) return null;
            InputStream in = conn.getInputStream();
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            in.close();
            Map<String, Object> resp = Json.toMap(new String(bos.toByteArray(), StandardCharsets.UTF_8));
            if (resp == null) return null;
            if (!"1".equals(String.valueOf(resp.get("code")))) {
                // 把 Python 给的真实原因（例如「生成失败：没有可用的封面图」）透出来，方便定位
                throw new BizException(String.valueOf(resp.get("msg")));
            }
            Object result = resp.get("result");
            return result instanceof Map ? (Map<String, Object>) result : null;
        } catch (java.io.IOException e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
