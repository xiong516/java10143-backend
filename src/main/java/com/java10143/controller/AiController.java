package com.java10143.controller;

import com.java10143.common.BizException;
import com.java10143.common.R;
import com.java10143.service.AiService;
import com.java10143.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 智能客服
 *   POST /member/ai/chat     body {message, orderId?, sessionId?}   聊天
 *   GET  /member/ai/status                                          查看 AI 服务是否在线
 *
 * 登录是「必须」的（2026-10-17 调整）：
 *   - 未登录访问 /member/ai/chat 会被 AuthInterceptor 直接拦下（401 请先登录）
 *   - 登录后自动带上你的真实订单上下文，AI 能回答“我这单到哪了”
 *   - /member/ai/status 仍保持公开，前端用它探测 AI 是否在线
 *
 * 前端不用直连 Python，走这里就行。
 */
@RestController
@RequestMapping("/member/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    public R<Map<String, Object>> chat(@RequestBody Map<String, Object> body) {
        Object msg = body.get("message");
        String message = msg == null ? "" : String.valueOf(msg).trim();
        if (message.isEmpty()) throw new BizException("请输入要问的内容");
        if (message.length() > 500) message = message.substring(0, 500);

        String orderId = body.get("orderId") == null ? null : String.valueOf(body.get("orderId"));
        String sessionId = body.get("sessionId") == null ? null : String.valueOf(body.get("sessionId"));

        // 必须登录：AuthInterceptor 已拦过一道，这里再用 require() 双保险
        return R.ok(aiService.chat(UserContext.require(), message, orderId, sessionId));
    }

    @GetMapping("/status")
    public R<Map<String, Object>> status() {
        return R.ok(aiService.status());
    }
}
