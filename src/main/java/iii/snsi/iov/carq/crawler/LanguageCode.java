package iii.snsi.iov.carq.crawler;

/**
 * Created by myco on 8/11/16.
 */
public enum LanguageCode {
    AUTO ("auto"),
    ENGLISH ("en"),
    SIMPLIFIED_CHINESE ("zh-CN"),
    TRADITIONAL_CHINESE ("zh-TW");

    private final String code;

    LanguageCode(String code) {
        this.code = code;
    }

    public String code(){
        return code;
    }
}
