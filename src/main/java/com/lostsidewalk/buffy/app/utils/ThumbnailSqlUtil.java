package com.lostsidewalk.buffy.app.utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.tomcat.util.http.fileupload.IOUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static com.lostsidewalk.buffy.app.utils.ThumbnailUtils.getImage;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

public class ThumbnailSqlUtil {

    private static final String SQL_INSERT_TEMPLATE = "insert into thumbnails (image_source) values ('%s');";

    private static final String BASE_PATH = "/home/me/tmp4/cats/";

    public static void main(String[] args) {
        // for ea. file in ~/tmp4/cats and ~/tmp4/office,
        // build image byte[] from ThumbnailUtils.getImage();
        // encode as base64, create insert statement

        List<String> fileNames = null;
        try {
            File tmp4 = new File(BASE_PATH);
            String[] s = tmp4.list();
            if (s != null) {
                fileNames = Arrays.stream(s).toList();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (isNotEmpty(fileNames)) {
            File output = new File(BASE_PATH + "all_inserts.sql");
            try {
                if (output.createNewFile()) {
                    FileWriter outputWriter = new FileWriter(output.getAbsolutePath());
                    PrintWriter printWriter = new PrintWriter(outputWriter);
                    for (String fileName : fileNames) {
                        try {
                            URL url = new File(BASE_PATH + fileName).toURI().toURL();
                            byte[] image = getImage(url.getPath(), url.openStream().readAllBytes(), 140);
                            String encodedImage = encodeBase64String(image);
                            String sqlInsert = String.format(SQL_INSERT_TEMPLATE, encodedImage);
                            printWriter.println(sqlInsert);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    printWriter.close();
                } else {
                    System.err.println("Unable to create output file");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
