package com.queueease.service;

import com.queueease.dto.ShopProfileDto;
import com.queueease.entity.Shop;
import com.queueease.entity.User;
import com.queueease.entity.enums.ShopStatus;

import java.util.List;

public interface ShopService {
    Shop createShop(User owner, String name, String slug, String address, String phone, String description, Integer avgMinutes);
    Shop getShopBySlug(String slug);
    Shop getShopById(Long id);
    Shop getShopByOwner(User owner);
    Shop updateShopProfile(Long shopId, ShopProfileDto dto);
    Shop updateShopStatus(Long shopId, ShopStatus status);
    List<Shop> getAllActiveShops();
}
