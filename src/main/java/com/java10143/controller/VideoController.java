package com.java10143.controller;

import com.java10143.common.R;
import com.java10143.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Map;

/**
 * 商品短视频
 *   GET  /home/video?limit=10        抖音式信息流（公开）
 *   GET  /home/video/{id}/like        点赞 +1（再调 /like/cancel 回退）
 *   GET  /home/video/{id}/collect     收藏 +1（再调 /collect/cancel 回退）
 *   GET  /home/video/{id}/share       分享 +1
 *   GET  /media/video/{file}         视频文件（支持 Range，边下边播 / 可拖动进度条）
 *
 * 视频文件放在工程的 media/video/ 目录下（jar 外，方便替换自己的视频）
 */
@RestController
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;
    private final com.java10143.service.VideoGenService videoGenService;

    @Value("${java10143.media.dir:media}")
    private String mediaDir;

    @GetMapping("/home/video")
    public R<List<Map<String, Object>>> feed(@org.springframework.web.bind.annotation.RequestParam(required = false) String limit) {
        return R.ok(videoService.feed(limit));
    }

    @org.springframework.web.bind.annotation.RequestMapping(value = "/home/video/{id}/like",
            method = {org.springframework.web.bind.annotation.RequestMethod.GET,
                      org.springframework.web.bind.annotation.RequestMethod.POST})
    public R<Map<String, Object>> like(@PathVariable String id) {
        return R.ok(videoService.like(Long.valueOf(id)));
    }

    /** 取消点赞 -1 */
    @org.springframework.web.bind.annotation.RequestMapping(value = "/home/video/{id}/like/cancel",
            method = {org.springframework.web.bind.annotation.RequestMethod.GET,
                      org.springframework.web.bind.annotation.RequestMethod.POST})
    public R<Map<String, Object>> likeCancel(@PathVariable String id) {
        return R.ok(videoService.likeCancel(Long.valueOf(id)));
    }

    /** 收藏 +1（GET 和 POST 都收，各端随便用） */
    @org.springframework.web.bind.annotation.RequestMapping(value = "/home/video/{id}/collect",
            method = {org.springframework.web.bind.annotation.RequestMethod.GET,
                      org.springframework.web.bind.annotation.RequestMethod.POST})
    public R<Map<String, Object>> collect(@PathVariable String id) {
        return R.ok(videoService.collect(Long.valueOf(id)));
    }

    /** 取消收藏 -1 */
    @org.springframework.web.bind.annotation.RequestMapping(value = "/home/video/{id}/collect/cancel",
            method = {org.springframework.web.bind.annotation.RequestMethod.GET,
                      org.springframework.web.bind.annotation.RequestMethod.POST})
    public R<Map<String, Object>> collectCancel(@PathVariable String id) {
        return R.ok(videoService.collectCancel(Long.valueOf(id)));
    }

    /** 分享 +1（GET 和 POST 都收） */
    @org.springframework.web.bind.annotation.RequestMapping(value = "/home/video/{id}/share",
            method = {org.springframework.web.bind.annotation.RequestMethod.GET,
                      org.springframework.web.bind.annotation.RequestMethod.POST})
    public R<Map<String, Object>> share(@PathVariable String id) {
        return R.ok(videoService.share(Long.valueOf(id)));
    }

    /**
     * 关键词生成 AI 短视频（管理员）
     * POST /admin/video/generate
     * body: {"keyword":"口红", "spuId":1001(可选，直接指定商品), "style":"seeds|unbox|flash|fresh"(可选), "duration":10(可选 5~20)}
     * 流程：选商品 → 拿 AI 商品图封面 → Python(8010) ffmpeg 按风格运镜+烧字幕合成 → 入库 video 表
     */
    @org.springframework.web.bind.annotation.PostMapping("/admin/video/generate")
    public R<Map<String, Object>> generate(@org.springframework.web.bind.annotation.RequestBody Map<String, Object> body) {
        return R.ok(videoGenService.generate(body));
    }

    /** AI 商品图等静态图片：GET /media/goods/{file}（PNG） */
    @GetMapping("/media/goods/{file}")
    public ResponseEntity<byte[]> goodsImage(@PathVariable String file) {
        File dir = new File(mediaDir + File.separator + "goods");
        File f = new File(dir, file);
        if (!f.exists() || !f.isFile()) {
            // 容错：数据库里可能记的是 .png，磁盘上实际是 .jpg（或反之），依次尝试其它后缀
            int dot = file.lastIndexOf('.');
            String base = dot > 0 ? file.substring(0, dot) : file;
            String[] alts = {".jpg", ".png", ".jpeg", ".webp"};
            for (String ext : alts) {
                File alt = new File(dir, base + ext);
                if (!file.equalsIgnoreCase(base + ext) && alt.exists() && alt.isFile()) {
                    f = alt;
                    break;
                }
            }
        }
        if (!f.exists() || !f.isFile()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        try {
            byte[] data = java.nio.file.Files.readAllBytes(f.toPath());
            HttpHeaders headers = new HttpHeaders();
            // 按真实文件后缀给 content-type：鸿蒙 Image 组件按 URL 后缀选解码器，
            // 后缀与字节格式不一致（.png 后缀 + JPEG 字节）会解码失败、图片空白。
            String lower = f.getName().toLowerCase();
            MediaType type = MediaType.IMAGE_PNG;
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) type = MediaType.IMAGE_JPEG;
            else if (lower.endsWith(".webp")) type = MediaType.parseMediaType("image/webp");
            headers.setContentType(type);
            headers.setCacheControl("public, max-age=86400");
            return new ResponseEntity<byte[]>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/media/video/{file}")
    public ResponseEntity<byte[]> media(@PathVariable String file,
                                        @RequestHeader(value = HttpHeaders.RANGE, required = false) String range) {
        // ★ 防路径穿越：只允许"纯文件名"（字母数字下划线横线点），
        //   挡住 ../../etc/passwd、..%2f..%2f 这类越权读取服务器任意文件
        if (file == null || !file.matches("^[A-Za-z0-9._-]{1,128}$") || file.contains("..")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        File dir = new File(mediaDir, "video");
        File f = new File(dir, file);
        try {
            // 再用规范路径二次校验：确保解析后的真实路径还在 video 目录里
            String basePath = dir.getCanonicalPath() + File.separator;
            if (!f.getCanonicalPath().startsWith(basePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (!f.exists() || !f.isFile()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        long total = f.length();
        try {
            long start = 0;
            long end = total - 1;
            if (range != null && range.startsWith("bytes=")) {
                String r = range.substring(6).trim();
                String[] parts = r.split("-");
                if (parts.length > 0 && !parts[0].isEmpty()) start = Long.parseLong(parts[0].trim());
                if (parts.length > 1 && !parts[1].isEmpty()) end = Long.parseLong(parts[1].trim());
                if (end >= total) end = total - 1;
                if (start > end || start < 0) start = 0;
            }

            int len = (int) (end - start + 1);
            byte[] buf = new byte[len];
            RandomAccessFile raf = new RandomAccessFile(f, "r");
            try {
                raf.seek(start);
                raf.readFully(buf);
            } finally {
                raf.close();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("video/mp4"));
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
            headers.setCacheControl("public, max-age=3600");
            if (range != null) {
                headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + total);
                headers.setContentLength(len);
                return new ResponseEntity<byte[]>(buf, headers, HttpStatus.PARTIAL_CONTENT);
            }
            headers.setContentLength(total);
            return new ResponseEntity<byte[]>(buf, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
