package com.java10143;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * Java10143 —— 鸿蒙商城 App（MyApplication10143）的配套后端
 *
 * 启动后（服务监听 0.0.0.0，电脑和手机都能访问）：
 *   电脑 / 手机   http://192.168.100.200:8080     （WLAN / WiFi 网卡）
 *   电脑 / 手机   http://192.168.103.21:8080      （有线网卡，同一网络里也能直连）
 *   本机          http://localhost:8080
 *   接口地址      http://192.168.100.200:8080/home/banner  等
 *   说明：以上地址"或"关系，哪个通就用哪个；网卡 IP 变了用 ipconfig 查新 IP。
 */
@SpringBootApplication
@MapperScan("com.java10143.mapper")
@ServletComponentScan
public class Java10143Application {

    public static void main(String[] args) {
        SpringApplication.run(Java10143Application.class, args);

        // 【修复】JDK 8 不支持文本块 (Text Blocks)，改为普通的字符串拼接
        System.out.println("\n" +
                "==========================================================\n" +
                "  Java10143 后端启动成功！\n" +
                "  电脑 + 手机 : http://192.168.100.200:8080/index.html   (WiFi)\n" +
                "  电脑 + 手机 : http://192.168.103.21:8080/index.html    (有线)\n" +
                "  直播广场   : http://192.168.100.200:8080/live.html\n" +
                "  数据大屏   : http://192.168.100.200:8080/admin-dashboard.html\n" +
                "  本机备用   : http://localhost:8080\n" +
                "  演示账号   : admin（管理员） / hm、test（普通用户）\n" +
                "==========================================================\n");
    }
}