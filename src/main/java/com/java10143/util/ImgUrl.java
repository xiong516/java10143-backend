package com.java10143.util;

import cn.hutool.core.util.StrUtil;

import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * 图片地址处理。
 *
 * 数据库里图片存的是相对路径，例如 /img/p/phone.svg
 * 这里在返回给客户端之前拼成绝对地址。
 * 好处：手机用 http://192.168.x.x:8080 访问后端时，
 * 返回的图片地址自动变成 http://192.168.x.x:8080/img/p/phone.svg，不用你手动改任何东西。
 */
public class ImgUrl {

    /**
     * 图片版本号：换一轮真实图片后把它 +1（v2 → v3 ...），
     * 所有端下发的图片地址就会变，浏览器/手机缓存立即失效，不用清缓存。
     */
    public static final String IMAGE_VERSION = "v2";

    public static String abs(String path) {
        if (path == null || StrUtil.isBlank(path)) return path;
        if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("data:")) {
            return path;
        }
        // 关键修复：把 /img/xxx.svg 统一改写成 .png 再下发。
        // 原因：鸿蒙 Image 组件按「URL 后缀」选解码器，.svg 后缀 + PNG 字节会解码失败，
        // 导致模拟器/真机上所有网络图片都不显示（本地资源图正常）。
        // ImgController 本来就不看后缀、一律输出真实图片内容，所以改成 .png 全平台通吃。
        // 注意数据库里存的路径带 ?t=... 查询串，只能替换问号前的文件后缀。
        if (path.contains("/img/")) {
            int q = path.indexOf('?');
            String p = q >= 0 ? path.substring(0, q) : path;
            if (p.endsWith(".svg")) {
                path = p.substring(0, p.length() - 4) + ".png" + (q >= 0 ? path.substring(q) : "");
            }
            // 追加版本参数打破缓存：有查询串用 &，没有用 ?
            path += (q >= 0 ? "&" : "?") + IMAGE_VERSION;
        }
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return path;
            HttpServletRequest req = attrs.getRequest();
            String host = req.getHeader("Host");
            if (host == null || StrUtil.isBlank(host)) host = req.getServerName() + ":" + req.getServerPort();
            return req.getScheme() + "://" + host + (path.startsWith("/") ? path : "/" + path);
        } catch (Exception e) {
            return path;
        }
    }

    public static List<String> abs(List<String> list) {
        List<String> res = new ArrayList<>();
        if (list == null) return res;
        for (String s : list) res.add(abs(s));
        return res;
    }

    /**
     * 兼容历史数据的归一化：订单表里可能存着旧端口/旧主机的绝对地址
     * （例如 http://localhost:8083/img/...，8083 是废弃副本）。
     * 这里把 scheme+host 剥掉只留路径，再按当前请求的 Host 重新拼成绝对地址，
     * 保证 PC / 手机 / 鸿蒙端拿到的图永远指向当前这台后端。
     */
    public static String normalize(String url) {
        if (url == null || StrUtil.isBlank(url)) return url;
        if (url.startsWith("http://") || url.startsWith("https://")) {
            int head = url.indexOf("://") + 3;
            int slash = url.indexOf('/', head);
            if (slash < 0) return url;
            url = url.substring(slash);
        }
        return abs(url);
    }


    /** 把 /media/video/xxx.mp4 这类媒体地址也拼成绝对地址（和后端同源） */
    public static String media(String path) {
        if (path == null || StrUtil.isBlank(path)) return path;
        if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("data:")) return path;
        if (path.startsWith("/")) return abs(path);
        return abs("/" + path);
    }
}
