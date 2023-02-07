package com.lostsidewalk.buffy.app.audit;

public class ProxyUrlHashException extends Exception {

    public ProxyUrlHashException(String url, String hash) {
        super(String.format("URL failed hash validation: url='%s', hash=%s", url, hash));
    }
}
