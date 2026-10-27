package com.java10143.controller;

import com.java10143.common.R;
import com.java10143.entity.Address;
import com.java10143.service.AddressService;
import com.java10143.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 收货地址
 *   GET    /member/address
 *   POST   /member/address           body 就是地址对象
 *   PUT    /member/address/{id}      body 就是地址对象
 *   DELETE /member/address/{id}
 */
@RestController
@RequestMapping("/member/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public R<List<Map<String, Object>>> list() {
        return R.ok(addressService.list(UserContext.require()));
    }

    @PostMapping
    public R<Map<String, Object>> add(@RequestBody Address address) {
        return R.ok(addressService.add(UserContext.require(), address));
    }

    @PutMapping("/{id}")
    public R<Map<String, Object>> update(@PathVariable String id, @RequestBody Address address) {
        return R.ok(addressService.update(UserContext.require(), Long.valueOf(id), address));
    }

    @DeleteMapping("/{id}")
    public R<Map<String, Object>> delete(@PathVariable String id) {
        return R.ok(addressService.delete(UserContext.require(), Long.valueOf(id)));
    }
}
