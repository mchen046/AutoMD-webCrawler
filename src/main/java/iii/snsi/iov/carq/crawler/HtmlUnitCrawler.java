/*
 * Copyright© Institute for Information Industry
 * All rights reserved.
 * 本程式碼係屬財團法人資訊工業策進會版權所有，在未取得本會書面同意前，不得複製、散佈或修改。
 */
package iii.snsi.iov.carq.crawler;

import java.io.IOException;
import java.net.MalformedURLException;

/*import org.slf4j.Logger;
import org.slf4j.LoggerFactory;*/

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * 程式資訊摘要：<P>
 * 類別名稱　　：HtmlUnitCrawler.java<P>
 * 程式內容說明：<P>
 * 程式修改記錄：<P>
 * XXXX-XX-XX：<P>
 *@author minglungweng
 *@version 1.0
 *@since 1.0
 */
public class HtmlUnitCrawler {
    //private static final Logger LOG = LoggerFactory.getLogger(HtmlUnitCrawler.class);

    private static ThreadLocal<WebClient> theWebClient = new ThreadLocal<WebClient>();
    
    public static WebClient getWebClient(boolean enableJs) {
        checkWebClientInit(enableJs);
        return theWebClient.get();
    }

    /**
     * @param enableJs 
     * 
     */
    private static void checkWebClientInit(boolean enableJs) {
        if(theWebClient.get()==null) {
            theWebClient.set(createWebClient(enableJs));
            //LOG.debug("theWebClient init for thread [{}]", Thread.currentThread().getId());
        }
    }
    
    public static WebClient createWebClient(boolean enableJs) {
        WebClient webClient = new WebClient(BrowserVersion.FIREFOX_45);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setPrintContentOnFailingStatusCode(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setJavaScriptEnabled(enableJs);
        webClient.getOptions().setUseInsecureSSL(true);
        return webClient;
    }
    
    
    public static HtmlPage getPage(String url, boolean enableJs) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
        //LOG.debug("crawling page... {}",url);
        HtmlPage page = getWebClient(enableJs).getPage(url);
        return page;
    }
    
    
    /**
     * return page if page is not null
     * @param url
     * @param enableJs
     * @return
     */
    public static HtmlPage getPageCatchEcept(String url, boolean enableJs) {
        HtmlPage page = null;
        try {
            page = getPage(url, enableJs);
        } catch (Exception e) {
            theWebClient.set(createWebClient(enableJs));
            //LOG.info("recreate theWebClient for thread [{}]", Thread.currentThread().getId());
        }
        return page;
    }
    
    public static HtmlPage getPageWithRetry(String url, boolean enableJs, int retry) {
        HtmlPage page;
        while(retry-->0) {
            page = getPageCatchEcept(url, enableJs);
            if(page!=null) {
                return page;
            }
            //LOG.debug("get page failed, retry="+retry);
        }
        return null;
    }
    
}
