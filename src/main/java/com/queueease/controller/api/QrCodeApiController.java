package com.queueease.controller.api;

import com.queueease.service.QrCodeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/qr")
public class QrCodeApiController {

    private final QrCodeService qrCodeService;

    public QrCodeApiController(QrCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
    }

    @GetMapping(value = "/{shopSlug}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getShopQrCode(
            @PathVariable("shopSlug") String shopSlug,
            @RequestParam(value = "size", defaultValue = "300") int size,
            @RequestParam(value = "download", defaultValue = "false") boolean download) {

        String queueUrl = qrCodeService.getShopQueueUrl(shopSlug);
        byte[] qrImage = qrCodeService.generateQrCodeImage(queueUrl, size, size);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentLength(qrImage.length);

        if (download) {
            headers.setContentDispositionFormData("attachment", shopSlug + "-qr.png");
        } else {
            headers.setContentDispositionFormData("inline", shopSlug + "-qr.png");
        }

        return new ResponseEntity<>(qrImage, headers, HttpStatus.OK);
    }
}
