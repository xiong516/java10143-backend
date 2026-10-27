package com.java10143.service;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.java10143.common.BizException;
import com.java10143.common.Guard;
import com.java10143.entity.User;
import com.java10143.mapper.UserMapper;
import com.java10143.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/** 登录 / 用户信息 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    /** POST /login  body: {account, password} */
    public Map<String, Object> login(String account, String password) {
        if (account == null || password == null) throw new BizException("用户名和密码不能为空");

        // ★ 防暴力破解：同一账号连续失败 5 次，锁 5 分钟（失败次数记在内存里）
        String guardKey = "login:" + account.trim().toLowerCase();
        long locked = Guard.lockLeft(guardKey);
        if (locked > 0) {
            long minutes = locked / 60000 + 1;
            throw new BizException("密码错误次数过多，请 " + minutes + " 分钟后再试");
        }

        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getAccount, account)
                .or()
                .eq(User::getMobile, account)
                .last("limit 1"));
        if (user == null) {
            Guard.fail(guardKey);
            throw new BizException("用户名或密码错误");
        }

        // 数据库里存的是 MD5，为了演示方便也兼容明文
        String md5 = SecureUtil.md5(password);
        if (!md5.equalsIgnoreCase(user.getPassword()) && !password.equals(user.getPassword())) {
            Guard.fail(guardKey);
            throw new BizException("用户名或密码错误");
        }
        Guard.success(guardKey);

        String token = jwtUtil.create(user.getId(), user.getAccount());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("account", user.getAccount());
        result.put("nickname", user.getNickname());
        result.put("avatar", user.getAvatar());
        result.put("mobile", user.getMobile());
        result.put("gender", user.getGender());
        result.put("birthday", user.getBirthday());
        result.put("cityCode", user.getCityCode());
        result.put("provinceCode", user.getProvinceCode());
        result.put("profession", user.getProfession());
        result.put("role", user.getRole() == null ? "user" : user.getRole());
        result.put("admin", "admin".equalsIgnoreCase(user.getRole()));
        result.put("token", token);
        return result;
    }

    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    /** 是否管理员：role 字段为 admin 才算 */
    public boolean isAdmin(Long userId) {
        if (userId == null) return false;
        User user = userMapper.selectById(userId);
        return user != null && "admin".equalsIgnoreCase(user.getRole());
    }

    /** 对外输出用户信息时统一带 role，避免各处漏填 */
    public Map<String, Object> profile(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (user == null) return map;
        map.put("id", user.getId());
        map.put("account", user.getAccount());
        map.put("nickname", user.getNickname());
        map.put("avatar", com.java10143.util.ImgUrl.normalize(user.getAvatar()));
        map.put("mobile", user.getMobile());
        map.put("gender", user.getGender());
        map.put("birthday", user.getBirthday());
        map.put("cityCode", user.getCityCode());
        map.put("provinceCode", user.getProvinceCode());
        map.put("profession", user.getProfession());
        map.put("role", user.getRole() == null ? "user" : user.getRole());
        map.put("admin", "admin".equalsIgnoreCase(user.getRole()));
        return map;
    }
}
