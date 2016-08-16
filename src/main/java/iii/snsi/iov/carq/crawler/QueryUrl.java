package iii.snsi.iov.carq.crawler;

/**
 * Created by myco on 8/10/16.
 */
public enum QueryUrl {
    BASEURL ("https://www.automd.com"),
    GOOGLETRANSLATEURL ("https://translate.google.com/?langpair={0}&text={1}");

    /*TRANSLATEURL ("http://api.microsofttranslator.com/v2/Http.svc/Translate"),
    TRANSLATEARRAYURL ("http://api.microsofttranslator.com/V2/Http.svc/TranslateArray"),
    ACCESSTOKENURL ("https://datamarket.accesscontrol.windows.net/v2/OAuth2-13");*/


    private final String url;

    QueryUrl(String url) {
        this.url = url;
    }

    public String url(){
        return url;
    }
}
