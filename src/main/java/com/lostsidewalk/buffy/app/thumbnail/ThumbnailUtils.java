package com.lostsidewalk.buffy.app.thumbnail;

import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.imgscalr.Scalr.Method.ULTRA_QUALITY;
import static org.imgscalr.Scalr.Mode.AUTOMATIC;

public class ThumbnailUtils {

    public static byte[] getImage(String path, InputStream inputStream, int targetSize) throws IOException {
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream)) {
            ImageReader reader = getImageReader(path);
            if (reader == null) {
                return null;
            } else {
                return getImage(reader, imageInputStream, targetSize);
            }
        }
    }

    private static byte[] getImage(ImageReader reader, ImageInputStream imageInputStream, int targetSize) throws IOException {
            String formatName = reader.getFormatName();
            reader.setInput(imageInputStream);
            BufferedImage bufferedImage = reader.read(0);
            BufferedImage thumbnailImage = Scalr.resize(bufferedImage, ULTRA_QUALITY, AUTOMATIC, targetSize, targetSize);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(thumbnailImage, formatName, baos);
            return baos.toByteArray();
    }

    private static ImageReader getImageReader(String path) {
        ImageReader ret = null;
        if (isNotBlank(path)) {
            int lastDotPos = path.lastIndexOf(".");
            if (lastDotPos > 0 && lastDotPos < path.length() - 1) {
                String suffix = path.substring(lastDotPos + 1);
                Iterator<?> readers = ImageIO.getImageReadersBySuffix(suffix);
                if (readers.hasNext()) {
                    ret = (ImageReader) readers.next();
                }
            }
        }
        return ret;
    }

    public static byte[] getImage(String imgSrc) {
        return decodeBase64(imgSrc);
    }
}
