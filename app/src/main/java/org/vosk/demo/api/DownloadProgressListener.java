package org.vosk.demo.api;

public interface DownloadProgressListener {
    void update(long bytesRead, long contentLength, boolean done);
}
