package com.example.thangcachep.movie_project_be.services.impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import lombok.extern.slf4j.Slf4j;

/**
 * Service để generate QR Code từ text/URL
 */
@Service
@Slf4j
public class QrCodeService {

    /**
     * Generate QR Code từ text và trả về base64 image
     *
     * @param text Text hoặc URL để encode vào QR code
     * @param width Chiều rộng QR code (pixels)
     * @param height Chiều cao QR code (pixels)
     * @return Base64 encoded image string (có thể dùng trực tiếp trong <img src="data:image/png;base64,...">)
     */
    public String generateQRCodeBase64(String text, int width, int height) {
        try {
            log.debug("🔲 Bắt đầu generate QR code - Text length: {}, Size: {}x{}", text.length(), width, height);

            // Tạo QR code writer
            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            // Cấu hình hints cho QR code
            java.util.Map<EncodeHintType, Object> hints = new java.util.HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // High error correction
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1); // Margin around QR code

            // Generate bit matrix từ text
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

            // Convert bit matrix thành BufferedImage
            BufferedImage qrImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    qrImage.setRGB(x, y, bitMatrix.get(x, y) ? 0x000000 : 0xFFFFFF); // Black or White
                }
            }

            // Convert BufferedImage thành byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", outputStream);
            byte[] imageBytes = outputStream.toByteArray();

            // Encode thành base64
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            log.info("✅ Đã generate QR code thành công - Size: {} bytes", imageBytes.length);
            return base64Image;

        } catch (WriterException e) {
            log.error("❌ Lỗi khi encode QR code: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể tạo QR code: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("❌ Lỗi khi convert image: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể convert QR code image: " + e.getMessage(), e);
        }
    }

    /**
     * Generate QR Code với kích thước mặc định (300x300)
     */
    public String generateQRCodeBase64(String text) {
        return generateQRCodeBase64(text, 300, 300);
    }

    /**
     * Generate QR Code và trả về data URL (có thể dùng trực tiếp trong HTML img tag)
     *
     * @param text Text hoặc URL để encode
     * @param width Chiều rộng (pixels)
     * @param height Chiều cao (pixels)
     * @return Data URL string: "data:image/png;base64,..."
     */
    public String generateQRCodeDataUrl(String text, int width, int height) {
        String base64 = generateQRCodeBase64(text, width, height);
        return "data:image/png;base64," + base64;
    }

    /**
     * Generate QR Code với data URL và kích thước mặc định
     */
    public String generateQRCodeDataUrl(String text) {
        return generateQRCodeDataUrl(text, 300, 300);
    }
}

