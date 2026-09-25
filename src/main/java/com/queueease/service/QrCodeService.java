package com.queueease.service;

public interface QrCodeService {
    byte[] generateQrCodeImage(String text, int width, int height);
    String getShopQueueUrl(String shopSlug);
}
