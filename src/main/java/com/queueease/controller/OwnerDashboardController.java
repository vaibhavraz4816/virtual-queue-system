package com.queueease.controller;

import com.queueease.dto.DashboardStatsDto;
import com.queueease.dto.OwnerQueueItemDto;
import com.queueease.dto.ShopProfileDto;
import com.queueease.dto.WalkInRequest;
import com.queueease.entity.QueueEntry;
import com.queueease.entity.Shop;
import com.queueease.entity.User;
import com.queueease.service.QrCodeService;
import com.queueease.service.QueueService;
import com.queueease.service.ShopService;
import com.queueease.service.UserService;
import com.queueease.util.DateTimeUtils;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/owner")
public class OwnerDashboardController {

    private final UserService userService;
    private final ShopService shopService;
    private final QueueService queueService;
    private final QrCodeService qrCodeService;

    public OwnerDashboardController(UserService userService,
                                    ShopService shopService,
                                    QueueService queueService,
                                    QrCodeService qrCodeService) {
        this.userService = userService;
        this.shopService = shopService;
        this.queueService = queueService;
        this.qrCodeService = qrCodeService;
    }

    private Shop getCurrentOwnerShop() {
        User owner = userService.getCurrentOwner();
        return shopService.getShopByOwner(owner);
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Shop shop = getCurrentOwnerShop();
        DashboardStatsDto stats = queueService.getDashboardStats(shop.getId());
        List<OwnerQueueItemDto> liveQueue = queueService.getLiveQueueForOwner(shop.getId());

        model.addAttribute("shop", shop);
        model.addAttribute("stats", stats);
        model.addAttribute("queue", liveQueue);
        model.addAttribute("walkInRequest", new WalkInRequest());
        model.addAttribute("queueUrl", qrCodeService.getShopQueueUrl(shop.getSlug()));
        return "owner/dashboard";
    }

    @GetMapping("/queue")
    public String queueView(Model model) {
        Shop shop = getCurrentOwnerShop();
        List<OwnerQueueItemDto> liveQueue = queueService.getLiveQueueForOwner(shop.getId());

        model.addAttribute("shop", shop);
        model.addAttribute("queue", liveQueue);
        model.addAttribute("walkInRequest", new WalkInRequest());
        return "owner/queue";
    }

    @GetMapping("/qr")
    public String qrCodeView(Model model) {
        Shop shop = getCurrentOwnerShop();
        String queueUrl = qrCodeService.getShopQueueUrl(shop.getSlug());

        model.addAttribute("shop", shop);
        model.addAttribute("queueUrl", queueUrl);
        return "owner/qr-code";
    }

    @GetMapping("/profile")
    public String profileView(Model model) {
        Shop shop = getCurrentOwnerShop();
        ShopProfileDto dto = new ShopProfileDto();
        dto.setId(shop.getId());
        dto.setName(shop.getName());
        dto.setSlug(shop.getSlug());
        dto.setAddress(shop.getAddress());
        dto.setPhone(shop.getPhone());
        dto.setDescription(shop.getDescription());
        dto.setAverageServiceMinutes(shop.getAverageServiceMinutes());
        dto.setOpeningTime(shop.getOpeningTime());
        dto.setClosingTime(shop.getClosingTime());
        dto.setStatus(shop.getStatus());

        model.addAttribute("shop", shop);
        model.addAttribute("profileDto", dto);
        return "owner/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("profileDto") ShopProfileDto dto,
                                BindingResult result,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        Shop shop = getCurrentOwnerShop();
        if (result.hasErrors()) {
            model.addAttribute("shop", shop);
            return "owner/profile";
        }

        try {
            shopService.updateShopProfile(shop.getId(), dto);
            redirectAttributes.addFlashAttribute("successMessage", "Shop profile updated successfully.");
            return "redirect:/owner/profile";
        } catch (Exception e) {
            model.addAttribute("shop", shop);
            model.addAttribute("errorMessage", e.getMessage());
            return "owner/profile";
        }
    }

    @GetMapping("/history")
    public String historyView(@RequestParam(value = "filter", defaultValue = "today") String filter, Model model) {
        Shop shop = getCurrentOwnerShop();
        List<QueueEntry> entries = queueService.getQueueHistory(shop.getId(), filter);

        model.addAttribute("shop", shop);
        model.addAttribute("entries", entries);
        model.addAttribute("currentFilter", filter);
        model.addAttribute("dateTimeUtils", new DateTimeUtils());
        return "owner/history";
    }

    @GetMapping("/statistics")
    public String statisticsView(Model model) {
        Shop shop = getCurrentOwnerShop();
        DashboardStatsDto stats = queueService.getDashboardStats(shop.getId());

        model.addAttribute("shop", shop);
        model.addAttribute("stats", stats);
        return "owner/statistics";
    }
}
