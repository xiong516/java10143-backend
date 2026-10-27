package com.java10143.controller;

import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;

/**
 * 图片服务：数据库里所有商品图、轮播图、分类图标都走这里。
 *
 * <p>取图顺序（★ 真实图片优先，不再把文字画在图上）：
 * <ol>
 *   <li>media/goods/g1001.jpg（商品真实图）</li>
 *   <li>media/banners/b1.jpg（轮播图真实海报）</li>
 *   <li>media/category/c101.jpg（分类图标）</li>
 *   <li>jar 内 classpath:/media/... 同名资源（换机器部署也能出图）</li>
 *   <li>都没有 → 才用 Java2D 画一张兜底图（保证任何情况下都不裂图）</li>
 * </ol>
 * 好处：四个客户端（鸿蒙 / WEB / uni-app / 微信小程序）用的都是同一个
 * {@code /img/p/p1001.png} 地址，后端换图后全端自动同步，前端一行都不用改。
 */
@RestController
@CrossOrigin(origins = "*")
public class ImgController {

    /** 和 VideoController 共用同一个 media 根目录（默认工程下的 media/） */
    @Value("${java10143.media.dir:media}")
    private String mediaDir;

    @GetMapping("/img/{type}/{file}")
    public ResponseEntity<byte[]> image(@PathVariable String type,
                                        @PathVariable String file,
                                        @RequestParam(required = false) String t,
                                        @RequestParam(required = false) String s,
                                        @RequestParam(required = false) Integer w,
                                        @RequestParam(required = false) Integer h) throws Exception {

        // 不管 URL 后缀是 .svg 还是 .png，一律按真实内容下发
        String seed = file.replace(".svg", "").replace(".png", "");

        // ① 先找真实图片（磁盘 media 目录优先，其次 jar 内 classpath）
        RealImg real = findReal(type, seed);
        if (real != null) {
            return ResponseEntity.ok()
                    .contentType(real.jpeg ? MediaType.IMAGE_JPEG : MediaType.IMAGE_PNG)
                    .header("Cache-Control", "public, max-age=3600")
                    .body(real.data);
        }

        // ② 没有真实图才兜底画一张（保证任何商品都不会裂图）
        int width = w != null ? w : defaultWidth(type);
        int height = h != null ? h : defaultHeight(type);
        String text = (t == null || StrUtil.isBlank(t)) ? seed : t;
        if (text.length() > 8) text = text.substring(0, 8);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                // 兜底图一天内直接走本地缓存，列表页几十张图也不卡
                .header("Cache-Control", "public, max-age=86400")
                .body(png(width, height, text, s, seed));
    }

    /** 读到的真实图片：字节 + 是否 JPEG */
    private static class RealImg {
        final byte[] data;
        final boolean jpeg;

        RealImg(byte[] data, boolean jpeg) {
            this.data = data;
            this.jpeg = jpeg;
        }
    }

    /**
     * 按业务类型找真实图片文件。
     * p = 商品图（media/goods/g + 数字.jpg）
     * b = 轮播图（media/banners/b + 数字.jpg）
     * c = 分类图标（media/category/c + 数字.jpg）
     * 先查 jar 外的 media 目录（方便随时换图），再查 jar 内 classpath（换机器也能出图）。
     */
    private RealImg findReal(String type, String seed) {
        String num = seed.replaceAll("\\D", "");
        if (StrUtil.isBlank(num)) return null;

        String sub;
        String prefix;
        if ("p".equals(type)) {
            sub = "goods";
            prefix = "g";
        } else if ("b".equals(type)) {
            sub = "banners";
            prefix = "b";
        } else if ("c".equals(type)) {
            sub = "category";
            prefix = "c";
        } else {
            return null;
        }

        String[] exts = {".jpg", ".jpeg", ".png"};

        // ① jar 外磁盘目录
        for (String ext : exts) {
            File f = new File(mediaDir + File.separator + sub + File.separator + prefix + num + ext);
            if (f.exists() && f.isFile()) {
                try {
                    return new RealImg(Files.readAllBytes(f.toPath()), isJpeg(ext));
                } catch (Exception ignore) {
                    // 读失败继续找下一个
                }
            }
        }

        // ② jar 内 classpath:/media/...
        for (String ext : exts) {
            String path = "/media/" + sub + "/" + prefix + num + ext;
            try (java.io.InputStream in = getClass().getResourceAsStream(path)) {
                if (in != null) {
                    ByteArrayOutputStream buf = new ByteArrayOutputStream();
                    byte[] tmp = new byte[8192];
                    int n;
                    while ((n = in.read(tmp)) > 0) buf.write(tmp, 0, n);
                    return new RealImg(buf.toByteArray(), isJpeg(ext));
                }
            } catch (Exception ignore) {
                // 继续下一个后缀
            }
        }
        return null;
    }

    private boolean isJpeg(String ext) {
        String e = ext.toLowerCase();
        return ".jpg".equals(e) || ".jpeg".equals(e);
    }

    private int defaultWidth(String type) {
        if ("b".equals(type)) return 750;
        if ("c".equals(type)) return 240;
        if ("a".equals(type)) return 160;
        return 600;
    }

    private int defaultHeight(String type) {
        if ("b".equals(type)) return 320;
        if ("c".equals(type)) return 240;
        if ("a".equals(type)) return 160;
        return 600;
    }

    private byte[] png(int w, int h, String text, String sub, String seed) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int hue = Math.abs(seed.hashCode()) % 360;
        Color c1 = Color.getHSBColor(hue / 360f, 0.62f, 0.92f);
        Color c2 = Color.getHSBColor(((hue + 45) % 360) / 360f, 0.78f, 0.66f);
        g.setPaint(new GradientPaint(0, 0, c1, w, h, c2));
        g.fillRect(0, 0, w, h);

        g.setColor(new Color(255, 255, 255, 34));
        g.fillOval((int) (w * 0.62), -(int) (h * 0.1), (int) (w * 0.55), (int) (h * 0.55));

        FontRenderContext frc = g.getFontRenderContext();
        int fontSize = Math.max(16, Math.min(w, h) / (text.length() > 4 ? 6 : 5));
        Font font = new Font("Microsoft YaHei", Font.BOLD, fontSize);
        Rectangle2D r = font.getStringBounds(text, frc);
        // 标题太长就缩字号，别顶出画面
        while (r.getWidth() > w * 0.86 && fontSize > 14) {
            fontSize -= 2;
            font = new Font("Microsoft YaHei", Font.BOLD, fontSize);
            r = font.getStringBounds(text, frc);
        }

        Font subFont = null;
        Rectangle2D r2 = null;
        if (sub != null && !StrUtil.isBlank(sub)) {
            int subSize = Math.max(12, fontSize / 2);
            subFont = new Font("Microsoft YaHei", Font.PLAIN, subSize);
            r2 = subFont.getStringBounds(sub, frc);
            while (r2.getWidth() > w * 0.88 && subSize > 11) {
                subSize -= 1;
                subFont = new Font("Microsoft YaHei", Font.PLAIN, subSize);
                r2 = subFont.getStringBounds(sub, frc);
            }
        }

        // 主标题 + 副标题作为一个整体垂直居中，中间留出明确间隙（以前两行会贴在一起）
        int gap = Math.max(10, h / 12);
        double blockH = r.getHeight() + (r2 == null ? 0 : gap + r2.getHeight());
        double top = (h - blockH) / 2.0;

        g.setFont(font);
        g.setColor(Color.WHITE);
        g.drawString(text, (int) Math.round((w - r.getWidth()) / 2),
                (int) Math.round(top - r.getY()));

        if (r2 != null) {
            g.setFont(subFont);
            g.setColor(new Color(255, 255, 255, 230));
            g.drawString(sub, (int) Math.round((w - r2.getWidth()) / 2),
                    (int) Math.round(top + r.getHeight() + gap - r2.getY()));
        }
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }
}