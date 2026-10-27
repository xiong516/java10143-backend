package com.java10143.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.java10143.common.BizException;
import com.java10143.entity.Address;
import com.java10143.mapper.AddressMapper;
import com.java10143.vo.Vo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 收货地址 */
@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressMapper addressMapper;

    public List<Map<String, Object>> list(Long userId) {
        List<Address> list = addressMapper.selectList(Wrappers.<Address>lambdaQuery()
                .eq(Address::getUserId, userId)
                .orderByAsc(Address::getIsDefault)
                .orderByDesc(Address::getId));
        List<Map<String, Object>> res = new ArrayList<>();
        for (Address a : list) res.add(Vo.address(a));
        return res;
    }

    @Transactional
    public Map<String, Object> add(Long userId, Address address) {
        address.setId(null);
        address.setUserId(userId);
        if (address.getIsDefault() == null) address.setIsDefault(1);
        if (address.getIsDefault() == 0) clearDefault(userId, null);
        addressMapper.insert(address);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", address.getId());
        return res;
    }

    @Transactional
    public Map<String, Object> update(Long userId, Long id, Address address) {
        Address exist = addressMapper.selectById(id);
        if (exist == null || !exist.getUserId().equals(userId)) throw new BizException("地址不存在");
        address.setId(id);
        address.setUserId(userId);
        if (address.getIsDefault() != null && address.getIsDefault() == 0) clearDefault(userId, id);
        addressMapper.updateById(address);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", id);
        return res;
    }

    public Map<String, Object> delete(Long userId, Long id) {
        Address exist = addressMapper.selectById(id);
        if (exist == null || !exist.getUserId().equals(userId)) throw new BizException("地址不存在");
        addressMapper.deleteById(id);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        return m;
    }

    private void clearDefault(Long userId, Long exceptId) {
        List<Address> list = addressMapper.selectList(Wrappers.<Address>lambdaQuery()
                .eq(Address::getUserId, userId).eq(Address::getIsDefault, 0));
        for (Address a : list) {
            if (exceptId != null && a.getId().equals(exceptId)) continue;
            a.setIsDefault(1);
            addressMapper.updateById(a);
        }
    }
}
