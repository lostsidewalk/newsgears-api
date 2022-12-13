package com.lostsidewalk.buffy.app.auth;

import com.google.common.hash.Hashing;

import java.nio.charset.Charset;

public class HashingUtils {

    public static String sha256(String str, Charset charset) {
        return Hashing.sha256().hashString(str, charset).toString();
    }
}
