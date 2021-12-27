package org.vosk.demo.api;

public class Download {

    public final static int CLEAR = 200;
    public final static int STARTING = 0;
    public final static int UNZIPPING = 202;
    public final static int COMPLETE = 203;
    public final static int RESTARTING = 204;

    private int progress;
    private long currentFileSize;
    private long totalFileSize;
    String modelName;

    public Download() {

    }

    public Download(int progress, String modelName) {
        this.progress = progress;
        this.modelName = modelName;
    }

    public Download(int progress, long currentFileSize, long totalFileSize) {
        this.progress = progress;
        this.currentFileSize = currentFileSize;
        this.totalFileSize = totalFileSize;
    }

    public Download(int progress) {
        this.progress = progress;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public long getCurrentFileSize() {
        return currentFileSize;
    }

    public void setCurrentFileSize(long currentFileSize) {
        this.currentFileSize = currentFileSize;
    }

    public long getTotalFileSize() {
        return totalFileSize;
    }

    public void setTotalFileSize(long totalFileSize) {
        this.totalFileSize = totalFileSize;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
}
