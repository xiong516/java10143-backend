package com.java10143.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.java10143.common.BizException;
import com.java10143.entity.Category;
import com.java10143.entity.Comment;
import com.java10143.entity.Orders;
import com.java10143.entity.Sku;
import com.java10143.entity.Spu;
import com.java10143.entity.User;
import com.java10143.entity.Video;
import com.java10143.mapper.CategoryMapper;
import com.java10143.mapper.CommentMapper;
import com.java10143.mapper.OrdersMapper;
import com.java10143.mapper.SkuMapper;
import com.java10143.mapper.SpuMapper;
import com.java10143.mapper.UserMapper;
import com.java10143.mapper.VideoMapper;
import com.java10143.util.ImgUrl;
import com.java10143.util.Json;
import com.java10143.vo.Vo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台：商品 / 用户 / 数据看板 的增删改查。
 * 权限由 AdminInterceptor 保证，这里只关心业务。
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final SpuMapper spuMapper;
    private final SkuMapper skuMapper;
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;
    private final OrdersMapper ordersMapper;
    private final CommentMapper commentMapper;
    private final VideoMapper videoMapper;
    private final VideoService videoService;
    private final CommentService commentService;

    // ------------------------------------------------ 数据看板

    /** GET /admin/stats 首页统计卡片 */
    public Map<String, Object> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("goodsCount", spuMapper.selectCount(null));
        m.put("userCount", userMapper.selectCount(null));
        m.put("orderCount", ordersMapper.selectCount(null));
        m.put("videoCount", videoMapper.selectCount(null));
        m.put("commentCount", commentMapper.selectCount(null));
        m.put("skuCount", skuMapper.selectCount(null));
        return m;
    }

    // ------------------------------------------------ 商品 CRUD

    /** GET /admin/goods?keyword=&page=&size= 商品列表（分页） */
    public Map<String, Object> goodsList(String keyword, String page, String size) {
        int p = intOf(page, 1);
        int s = intOf(size, 10);
        if (p < 1) p = 1;
        if (s < 1) s = 10;
        if (s > 100) s = 100;

        LambdaQueryWrapper<Spu> q = Wrappers.<Spu>lambdaQuery().orderByDesc(Spu::getId);
        if (!StrUtil.isBlank(keyword)) {
            q.and(w -> w.like(Spu::getName, keyword).or().like(Spu::getSpuCode, keyword));
        }
        long total = spuMapper.selectCount(q);
        q.last("limit " + ((p - 1) * s) + "," + s);
        List<Spu> list = spuMapper.selectList(q);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Spu spu : list) rows.add(Vo.product(spu));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("total", total);
        res.put("page", p);
        res.put("size", s);
        res.put("rows", rows);
        return res;
    }

    /** GET /admin/goods/{id} 单个商品（后台编辑用，字段更全） */
    public Map<String, Object> goodsDetail(Long id) {
        Spu spu = spuMapper.selectById(id);
        if (spu == null) throw new BizException("商品不存在");
        Map<String, Object> m = Vo.product(spu);
        m.put("categoryId", spu.getCategoryId());
        m.put("brandId", spu.getBrandId());
        m.put("inventory", spu.getInventory());
        m.put("isPreSale", spu.getIsPreSale());
        m.put("videoScale", spu.getVideoScale());
        m.put("mainPictures", Json.strList(spu.getMainPictures()));
        m.put("detailPictures", Json.strList(spu.getDetailPictures()));
        m.put("mainVideos", Json.strList(spu.getMainVideos()));
        m.put("specs", Json.mapList(spu.getSpecs()));
        m.put("detailProperties", Json.mapList(spu.getDetailProperties()));
        return m;
    }

    /** POST /admin/goods 新增商品 */
    public Map<String, Object> goodsCreate(Map<String, Object> body) {
        Spu spu = new Spu();
        applyGoods(spu, body, true);
        spuMapper.insert(spu);
        // 顺带建一个默认 SKU，保证下单流程可用
        createDefaultSku(spu);
        return goodsDetail(spu.getId());
    }

    /** PUT /admin/goods/{id} 修改商品 */
    public Map<String, Object> goodsUpdate(Long id, Map<String, Object> body) {
        Spu spu = spuMapper.selectById(id);
        if (spu == null) throw new BizException("商品不存在");
        applyGoods(spu, body, false);
        spuMapper.updateById(spu);
        return goodsDetail(id);
    }

    /** DELETE /admin/goods/{id} 删除商品（连 SKU 一起删） */
    public Boolean goodsDelete(Long id) {
        spuMapper.deleteById(id);
        skuMapper.delete(Wrappers.<Sku>lambdaQuery().eq(Sku::getSpuId, id));
        return true;
    }

    /** GET /admin/categories 分类下拉（后台新增商品用） */
    public List<Map<String, Object>> categories() {
        List<Category> list = categoryMapper.selectList(
                Wrappers.<Category>lambdaQuery().orderByAsc(Category::getLayer).orderByAsc(Category::getId));
        List<Map<String, Object>> res = new ArrayList<>();
        for (Category c : list) res.add(Vo.category(c));
        return res;
    }

    // ------------------------------------------------ 视频 CRUD（委托 VideoService）

    public List<Map<String, Object>> videoList() {
        return videoService.adminList();
    }

    public Map<String, Object> videoCreate(Map<String, Object> body) {
        return videoService.create(body);
    }

    public Map<String, Object> videoUpdate(Long id, Map<String, Object> body) {
        return videoService.update(id, body);
    }

    public Boolean videoDelete(Long id) {
        return videoService.delete(id);
    }

    // ------------------------------------------------ 评论 CRUD（委托 CommentService）

    public List<Map<String, Object>> commentList(String targetType, String keyword) {
        return commentService.adminList(targetType, keyword);
    }

    public Map<String, Object> commentUpdate(Long id, Map<String, Object> body) {
        return commentService.adminUpdate(id, body);
    }

    public Boolean commentDelete(Long id) {
        return commentService.adminDelete(id);
    }

    // ------------------------------------------------ 用户 CRUD

    /** GET /admin/user?keyword= 用户列表 */
    public List<Map<String, Object>> userList(String keyword) {
        LambdaQueryWrapper<User> q = Wrappers.<User>lambdaQuery().orderByDesc(User::getId).last("limit 200");
        if (!StrUtil.isBlank(keyword)) {
            q.and(w -> w.like(User::getAccount, keyword).or().like(User::getNickname, keyword));
        }
        List<User> list = userMapper.selectList(q);
        List<Map<String, Object>> res = new ArrayList<>();
        for (User u : list) res.add(toUserRow(u));
        return res;
    }

    /** POST /admin/user 新增用户 */
    public Map<String, Object> userCreate(Map<String, Object> body) {
        String account = str(body.get("account"));
        String password = str(body.get("password"));
        if (StrUtil.isBlank(account)) throw new BizException("账号不能为空");
        if (StrUtil.isBlank(password)) password = "123456";
        Long exists = userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getAccount, account));
        if (exists != null && exists > 0) throw new BizException("账号已存在");

        User u = new User();
        u.setAccount(account);
        u.setPassword(cn.hutool.crypto.SecureUtil.md5(password));
        u.setNickname(StrUtil.isBlank(str(body.get("nickname"))) ? account : str(body.get("nickname")));
        u.setAvatar(str(body.get("avatar")));
        u.setMobile(str(body.get("mobile")));
        u.setGender(str(body.get("gender")));
        u.setRole("admin".equalsIgnoreCase(str(body.get("role"))) ? "admin" : "user");
        userMapper.insert(u);
        return toUserRow(userMapper.selectById(u.getId()));
    }

    /** PUT /admin/user/{id} 修改用户（昵称/手机/角色；传 password 才改密码） */
    public Map<String, Object> userUpdate(Long id, Map<String, Object> body) {
        User u = userMapper.selectById(id);
        if (u == null) throw new BizException("用户不存在");
        if (body.containsKey("nickname")) u.setNickname(str(body.get("nickname")));
        if (body.containsKey("avatar")) u.setAvatar(str(body.get("avatar")));
        if (body.containsKey("mobile")) u.setMobile(str(body.get("mobile")));
        if (body.containsKey("gender")) u.setGender(str(body.get("gender")));
        if (body.containsKey("role")) u.setRole("admin".equalsIgnoreCase(str(body.get("role"))) ? "admin" : "user");
        String pwd = str(body.get("password"));
        if (!StrUtil.isBlank(pwd)) u.setPassword(cn.hutool.crypto.SecureUtil.md5(pwd));
        userMapper.updateById(u);
        return toUserRow(userMapper.selectById(id));
    }

    /** DELETE /admin/user/{id} 删除用户 */
    public Boolean userDelete(Long id) {
        User u = userMapper.selectById(id);
        if (u != null && "admin".equalsIgnoreCase(u.getRole())) {
            throw new BizException("管理员账号不允许删除");
        }
        userMapper.deleteById(id);
        return true;
    }

    // ------------------------------------------------ 订单（只读列表，方便后台看单）
    public List<Map<String, Object>> orderList(String keyword) {
        LambdaQueryWrapper<Orders> q = Wrappers.<Orders>lambdaQuery().orderByDesc(Orders::getId).last("limit 200");
        List<Orders> list = ordersMapper.selectList(q);
        List<Map<String, Object>> res = new ArrayList<>();
        for (Orders o : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", o.getId());
            m.put("orderState", o.getOrderState());
            m.put("payState", o.getPayState());
            m.put("payMoney", o.getPayMoney());
            m.put("totalNum", o.getTotalNum());
            m.put("receiver", o.getReceiver());
            m.put("receiverContact", o.getReceiverContact());
            m.put("createTime", o.getCreateTime() == null ? null : o.getCreateTime().toString().replace("T", " "));
            res.add(m);
        }
        return res;
    }

    // ------------------------------------------------ 内部

    private Map<String, Object> toUserRow(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("account", u.getAccount());
        m.put("nickname", u.getNickname());
        m.put("avatar", ImgUrl.abs(u.getAvatar()));
        m.put("mobile", u.getMobile());
        m.put("gender", u.getGender());
        m.put("role", u.getRole() == null ? "user" : u.getRole());
        m.put("createTime", u.getCreateTime() == null ? null : u.getCreateTime().toString().replace("T", " "));
        return m;
    }

    /** 新增/修改商品字段映射 */
    private void applyGoods(Spu spu, Map<String, Object> body, boolean create) {
        if (create || body.containsKey("name")) {
            String name = str(body.get("name"));
            if (StrUtil.isBlank(name)) throw new BizException("商品名称不能为空");
            spu.setName(name);
        }
        if (body.containsKey("spuCode")) spu.setSpuCode(str(body.get("spuCode")));
        if (body.containsKey("description")) spu.setDescription(str(body.get("description")));
        if (body.containsKey("price")) spu.setPrice(decimalOf(body.get("price"), "0.00"));
        if (body.containsKey("oldPrice")) spu.setOldPrice(decimalOf(body.get("oldPrice"), "0.00"));
        if (body.containsKey("categoryId")) spu.setCategoryId(longOf(body.get("categoryId")));
        if (body.containsKey("brandId")) spu.setBrandId(longOf(body.get("brandId")));
        if (body.containsKey("inventory")) spu.setInventory(intOf(body.get("inventory"), 0));
        if (body.containsKey("salesCount")) spu.setSalesCount(intOf(body.get("salesCount"), 0));
        if (body.containsKey("isPreSale")) spu.setIsPreSale(intOf(body.get("isPreSale"), 0));
        if (body.containsKey("videoScale")) spu.setVideoScale(intOf(body.get("videoScale"), 1));
        if (body.containsKey("mainPictures")) spu.setMainPictures(Json.to(toStrList(body.get("mainPictures"))));
        if (body.containsKey("detailPictures")) spu.setDetailPictures(Json.to(toStrList(body.get("detailPictures"))));
        if (body.containsKey("mainVideos")) spu.setMainVideos(Json.to(toStrList(body.get("mainVideos"))));
        if (spu.getCommentCount() == null) spu.setCommentCount(0);
        if (spu.getCollectCount() == null) spu.setCollectCount(0);
        if (spu.getSalesCount() == null) spu.setSalesCount(0);
        if (spu.getInventory() == null) spu.setInventory(0);
        if (spu.getVideoScale() == null) spu.setVideoScale(1);
    }

    /** 给新商品补一个默认 SKU，否则购物车/下单会走不通 */
    private void createDefaultSku(Spu spu) {
        try {
            Sku sku = new Sku();
            sku.setSpuId(spu.getId());
            sku.setSkuCode(spu.getSpuCode() == null ? ("SKU" + spu.getId()) : (spu.getSpuCode() + "-1"));
            sku.setPrice(spu.getPrice());
            sku.setOldPrice(spu.getOldPrice());
            sku.setInventory(spu.getInventory() == null ? 0 : spu.getInventory());
            sku.setSpecs("[{\"name\":\"规格\",\"valueName\":\"默认\"}]");
            skuMapper.insert(sku);
        } catch (Exception ignored) {
        }
    }

    private List<String> toStrList(Object o) {
        List<String> res = new ArrayList<>();
        if (o == null) return res;
        if (o instanceof List) {
            for (Object x : (List<?>) o) {
                if (x != null && !StrUtil.isBlank(String.valueOf(x))) res.add(String.valueOf(x));
            }
            return res;
        }
        String s = String.valueOf(o).trim();
        if (StrUtil.isBlank(s)) return res;
        if (s.startsWith("[")) return Json.strList(s);
        for (String x : s.split(",")) {
            if (!StrUtil.isBlank(x)) res.add(x.trim());
        }
        return res;
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private Long longOf(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o).trim();
        if (s.isEmpty() || "null".equals(s)) return null;
        try {
            return Long.valueOf(s);
        } catch (Exception e) {
            return null;
        }
    }

    private int intOf(Object o, int def) {
        try {
            return o == null ? def : (int) Double.parseDouble(String.valueOf(o));
        } catch (Exception e) {
            return def;
        }
    }

    private BigDecimal decimalOf(Object o, String def) {
        try {
            return o == null ? new BigDecimal(def) : new BigDecimal(String.valueOf(o));
        } catch (Exception e) {
            return new BigDecimal(def);
        }
    }
}
