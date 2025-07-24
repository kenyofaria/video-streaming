package com.streaming.example.service;

import org.springframework.core.io.AbstractResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public class ByteRangeResource extends AbstractResource {
    private final Path path;
    private final long start;
    private final long contentLength;

    public ByteRangeResource(Path path, long start, long contentLength) {
        this.path = path;
        this.start = start;
        this.contentLength = contentLength;
    }

    @Override
    public String getDescription() {
        return "Byte range [" + start + "-" + (start + contentLength - 1) + "] of " + path.getFileName();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        FileInputStream fis = new FileInputStream(path.toFile());
        fis.skip(start);
        return new BoundedInputStream(fis, contentLength);
    }

    @Override
    public long contentLength() {
        return contentLength;
    }

    @Override
    public boolean exists() {
        return path.toFile().exists();
    }

    /**
     * InputStream wrapper that limits reading to specified number of bytes
     */
    private static class BoundedInputStream extends InputStream {
        private final InputStream wrapped;
        private long remaining;

        public BoundedInputStream(InputStream wrapped, long maxBytes) {
            this.wrapped = wrapped;
            this.remaining = maxBytes;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int result = wrapped.read();
            if (result != -1) {
                remaining--;
            }
            return result;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            len = (int) Math.min(len, remaining);
            int result = wrapped.read(b, off, len);
            if (result != -1) {
                remaining -= result;
            }
            return result;
        }

        @Override
        public void close() throws IOException {
            wrapped.close();
        }
    }
}
