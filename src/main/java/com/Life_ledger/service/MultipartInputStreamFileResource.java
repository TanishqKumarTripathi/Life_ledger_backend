package com.Life_ledger.service;

import org.springframework.core.io.InputStreamResource;

import java.io.IOException;
import java.io.InputStream;

public class MultipartInputStreamFileResource extends InputStreamResource {

    private final String filename;

    public MultipartInputStreamFileResource(InputStream inputStream, String filename) {
        super(inputStream);
        this.filename = filename;
    }

    @Override
    public String getFilename() {
        return this.filename;
    }

    /**
     * Return -1 to let RestTemplate set Transfer-Encoding: chunked.
     * If you know content length, return it.
     */
    @Override
    public long contentLength() throws IOException {
        return -1;
    }
}
