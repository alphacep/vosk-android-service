package org.vosk.service.download;

public interface DownloadProgressListener {
    void update(long bytesRead, long contentLength, boolean done);
}
