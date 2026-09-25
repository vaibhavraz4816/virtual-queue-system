package com.queueease.repository;

import com.queueease.entity.Shop;
import com.queueease.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {
    Optional<Shop> findBySlug(String slug);
    List<Shop> findByOwner(User owner);
    List<Shop> findByOwnerId(Long ownerId);
    boolean existsBySlug(String slug);
}
