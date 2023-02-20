package com.lostsidewalk.buffy.app.utils;

import lombok.extern.slf4j.Slf4j;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static javax.imageio.ImageIO.createImageInputStream;
import static javax.imageio.ImageIO.write;
import static org.imgscalr.Scalr.Method.ULTRA_QUALITY;
import static org.imgscalr.Scalr.Mode.AUTOMATIC;

@Slf4j
public class ThumbnailUtils {

    public static byte[] getImage(String path, byte[] inputSrc, int targetSize) {
        try {
            ImageInputStream imageInputStream = createImageInputStream(new ByteArrayInputStream(inputSrc));
            return getImage(imageInputStream, targetSize);
        } catch (Exception e) {
            log.warn("Image decoding of path={} using failed due to: {}", path, e.getMessage());
        }

        return null;
    }

    private static byte[] getImage(ImageInputStream imageInputStream, int targetSize) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(imageInputStream);
        boolean skipResize = bufferedImage.getWidth() <= targetSize && bufferedImage.getHeight() <= targetSize;
        BufferedImage thumbnailImage = skipResize ? bufferedImage :
                Scalr.resize(bufferedImage, ULTRA_QUALITY, AUTOMATIC, targetSize, targetSize);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        write(thumbnailImage, "png", baos);
        return baos.toByteArray();
    }
}
