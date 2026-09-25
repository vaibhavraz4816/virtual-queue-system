package com.queueease.service.impl;

import com.queueease.dto.ShopProfileDto;
import com.queueease.entity.Shop;
import com.queueease.entity.User;
import com.queueease.entity.enums.ShopStatus;
import com.queueease.exception.ResourceNotFoundException;
import com.queueease.repository.ShopRepository;
import com.queueease.service.ShopService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ShopServiceImpl implements ShopService {

    private final ShopRepository shopRepository;

    public ShopServiceImpl(ShopRepository shopRepository) {
        this.shopRepository = shopRepository;
    }

    @Override
    @Transactional
    public Shop createShop(User owner, String name, String slug, String address, String phone, String description, Integer avgMinutes) {
        Shop shop = new Shop(owner, name, slug, address, phone, description, avgMinutes);
        return shopRepository.save(shop);
    }

    @Override
    public Shop getShopBySlug(String slug) {
        return shopRepository.findBySlug(slug != null ? slug.trim().toLowerCase() : "")
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found for slug: " + slug));
    }

    @Override
    public Shop getShopById(Long id) {
        return shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with ID: " + id));
    }

    @Override
    public Shop getShopByOwner(User owner) {
        List<Shop> shops = shopRepository.findByOwner(owner);
        if (shops.isEmpty()) {
            throw new ResourceNotFoundException("No shop found for owner: " + owner.getEmail());
        }
        return shops.get(0);
    }

    @Override
    @Transactional
    public Shop updateShopProfile(Long shopId, ShopProfileDto dto) {
        Shop shop = getShopById(shopId);
        shop.setName(dto.getName().trim());
        shop.setAddress(dto.getAddress());
        shop.setPhone(dto.getPhone());
        shop.setDescription(dto.getDescription());
        if (dto.getAverageServiceMinutes() != null && dto.getAverageServiceMinutes() > 0) {
            shop.setAverageServiceMinutes(dto.getAverageServiceMinutes());
        }
        if (dto.getOpeningTime() != null) {
            shop.setOpeningTime(dto.getOpeningTime());
        }
        if (dto.getClosingTime() != null) {
            shop.setClosingTime(dto.getClosingTime());
        }
        if (dto.getStatus() != null) {
            shop.setStatus(dto.getStatus());
        }
        if (dto.getCategory() != null) {
            shop.setCategory(dto.getCategory());
        }
        return shopRepository.save(shop);
    }

    @Override
    @Transactional
    public Shop updateShopStatus(Long shopId, ShopStatus status) {
        Shop shop = getShopById(shopId);
        shop.setStatus(status);
        return shopRepository.save(shop);
    }

    @Override
    public List<Shop> getAllActiveShops() {
        return shopRepository.findAll();
    }
}
