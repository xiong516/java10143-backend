package com.java10143.controller;

import com.java10143.common.R;
import com.java10143.entity.Spu;
import com.java10143.entity.Video;
import com.java10143.mapper.SpuMapper;
import com.java10143.mapper.VideoMapper;
import com.java10143.vo.Vo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 短视频带货（内容电商闭环）
 *   GET /home/video/{id}/goods   取这条视频的"同款商品"，没有挂商品时按类目/热销推荐兜底
 *
 * 数据来源：video.spu_id → spu（视频本来就挂了商品 ID，
 * 所以不需要额外建表，只是把两个模块缝在一起）。
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/home/video")
@RequiredArgsConstructor
public class VideoGoodsController {

    private final VideoMapper videoMapper;
    private final SpuMapper spuMapper;

    @GetMapping("/{id}/goods")
    public R<Map<String, Object>> goods(@PathVariable("id") Long id) {
        Video v = videoMapper.selectById(id);
        if (v == null) {
            return R.fail("视频不存在");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("videoId", v.getId());
        m.put("title", v.getTitle());

        Spu main = v.getSpuId() == null ? null : spuMapper.selectById(v.getSpuId());
        if (main != null) {
            m.put("matched", true);
            m.put("goods", card(main));
            // 同店搭配：同类目其它 2 件（详情页会用，前端也能展示"搭配买"）
            m.put("related", related(main));
        } else {
            m.put("matched", false);
            m.put("goods", null);
            m.put("related", hotFallback());
        }
        return R.ok(m);
    }

    /** 同类目其它商品，最多 4 条 */
    private List<Map<String, Object>> related(Spu main) {
        List<Spu> list = spuMapper.selectList(null);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Spu s : list) {
            if (s.getId().equals(main.getId())) continue;
            if (main.getCategoryId() != null && main.getCategoryId().equals(s.getCategoryId())) {
                out.add(card(s));
                if (out.size() >= 4) break;
            }
        }
        return out;
    }

    /** 视频没挂商品时的兜底：按销量取 4 件 */
    private List<Map<String, Object>> hotFallback() {
        List<Spu> list = spuMapper.selectList(null);
        list.sort((a, b) -> Integer.compare(
                b.getSalesCount() == null ? 0 : b.getSalesCount(),
                a.getSalesCount() == null ? 0 : a.getSalesCount()));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Spu s : list) {
            out.add(card(s));
            if (out.size() >= 4) break;
        }
        return out;
    }

    /** 统一成前端商品卡片需要的字段（复用 Vo.product，图片/价格格式与首页一致） */
    private Map<String, Object> card(Spu s) {
        return Vo.product(s);
    }
}
