package com.queueease.controller;

import com.queueease.entity.Shop;
import com.queueease.service.ShopService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private final ShopService shopService;

    public HomeController(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<Shop> sampleShops = shopService.getAllActiveShops();
        model.addAttribute("shops", sampleShops);
        return "index";
    }
}
