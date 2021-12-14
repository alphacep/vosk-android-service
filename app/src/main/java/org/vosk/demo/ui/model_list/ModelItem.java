package org.vosk.demo.ui.model_list;

public class ModelItem {
    private String lang;
    private String lang_text;
    private String md5;
    private String name;
    private boolean obsolete;
    private long size;
    private String size_text;
    private String type;
    private String url;
    private String version;

    public ModelItem() {
    }

    public ModelItem(String modelName) {
        this.name = modelName;
    }

    public ModelItem(String lang, String lang_text, String md5, String name, boolean obsolete, long size, String size_text, String type, String url, String version) {
        this.lang = lang;
        this.lang_text = lang_text;
        this.md5 = md5;
        this.name = name;
        this.obsolete = obsolete;
        this.size = size;
        this.size_text = size_text;
        this.type = type;
        this.url = url;
        this.version = version;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getLang_text() {
        return lang_text;
    }

    public void setLang_text(String lang_text) {
        this.lang_text = lang_text;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getObsolete() {
        return obsolete;
    }

    public void setObsolete(boolean obsolete) {
        this.obsolete = obsolete;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getSize_text() {
        return size_text;
    }

    public void setSize_text(String size_text) {
        this.size_text = size_text;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
