/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package alr.searchengine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import javax.net.ssl.SSLHandshakeException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Herbe_000
 */
public class DefaultCrawler extends Thread implements Crawler {

//---------------------------------------------------------object attributes---------------------------------------------------------
    private List<String> urlCache;
    private Coordinator coordinator;
    private boolean[] params;
    private int docCounter = 0;
    private String docName = "";
    private List<String> urls = new ArrayList<>();
    private Map<String, MutableInt> backLinks = new HashMap<>();
    private final Logger LOGGER = LoggerFactory.getLogger(DefaultCrawler.class);
    private int maxLvl;

//---------------------------------------------------------constructors---------------------------------------------------------
    public DefaultCrawler(List<String> urlCache, Coordinator coordinator, int maxLvl) {
        this.urlCache = urlCache;
        this.coordinator = coordinator;
        LOGGER.info("created crawler with urlcache: " + Arrays.toString(urlCache.toArray()));
        this.maxLvl = maxLvl;
    }

    public DefaultCrawler(List<String> urlCache, Coordinator coordinator, boolean[] params) {
        this.urlCache = urlCache;
        this.coordinator = coordinator;
        this.params = params;
    }

//---------------------------------------------------------public methods---------------------------------------------------------
    @Override
    public void run() {
        while (!urlCache.isEmpty() && !interrupted()) {
            for (String element : urlCache) {
                if (!interrupted()) {
                    try {
                        crawl(element, element.replace("http://", "").replace("https://", "").replace("www.", ""));
                    } catch (SQLException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            urlCache = coordinator.getUrlCacheForCrawler();
        }
    }

    @Override
    public void finish() {

    }

    @Override
    public void transmitBacklinksAndAdjustRating() {
        ConcurrentMap<String, MutableInt> siteRating = coordinator.getSiteRating();
        for (String backLink : backLinks.keySet()) {
            MutableInt count = siteRating.get(backLink);
            if (count == null) {
                siteRating.put(backLink, new MutableInt());
            } else {
                count.incrementBy(backLinks.get(backLink).get());
            }
        }
    }

    @Override
    public void setNewUrlCache(List<String> urlCache) {
        this.urlCache = urlCache;
    }

//---------------------------------------------------------private methods---------------------------------------------------------
    private void crawl(String url, String baseUrl) throws SQLException, IOException {
       
        if (url == null || !url.contains(baseUrl) || url.isEmpty() || url.contains("@") || url.contains(".jpg") || url.contains(".html#") || url.contains(".gif") || url.contains(".png")) {
           
            return; // do nothing
        }
        String docPath = coordinator.getDocPath();
        if (!new File(docPath).exists()) {
            new File(docPath).mkdir();
        }
        Document doc = null;
        //get site and ignore HTTP Errors ( 404 etc)

        Writer writer = null;
        //when url contains some documents
        if (url.contains(".pdf")
                || url.contains(".doc")
                || url.contains(".xlsx")
                || url.contains(".xls")) {
            //toDo machbar?
            return;
        }
        long start = System.currentTimeMillis();
        try {
            doc = Jsoup.connect(url).ignoreHttpErrors(true).timeout(10 * 100).get();
        } catch (SocketTimeoutException ex) {
            coordinator.addToQueue(url);
            return;
        } catch (java.net.ConnectException ex) {
            coordinator.addToQueue(url);
            return;
        } catch (SSLHandshakeException ex) {
            LOGGER.error("SSL Exception for " + url);
            urls.add(url);
            return;
        } catch (Throwable t) {
            LOGGER.error("Exception while connecting to " + url, t);
            urls.add(url);
            return;

        }
        LOGGER.info("getting doc " + (System.currentTimeMillis() - start));
        docCounter++;
        //save as text as txt
        try {
            String fileName = generateFileName(url);
            String name;
            File file = new File(fileName);
            if (fileName.contains("/")) {
                int last = fileName.lastIndexOf("/");
                name = fileName.substring(0, last);
                File directory = new File(name);
                directory.mkdirs();
            }

            try (BufferedWriter output = new BufferedWriter(new FileWriter(file))) {
                output.write(doc.text());

            }
            urls.add(url);
        } catch (IOException e) {
            System.out.println("file could not be safed " + url);
            e.printStackTrace();
        }
        //get all links and recursively call the processPage method
        Elements questions = doc.select("a[href]");
        List<String> nextLinksIntern = new ArrayList<>();
        List<String> nextLinksExtern = new ArrayList<>();
        for (Element link : questions) {
            String nextLink = link.attr("abs:href");
            int lvlCount = nextLink.length() - nextLink.replace("//", "").length();
            if (maxLvl != -1 && lvlCount > maxLvl) {
                break;
            }
            if (nextLink.contains(baseUrl)&&!nextLinksIntern.contains(nextLink)) {
                nextLinksIntern.add(nextLink);
            } else {
                String nextBaseUrl = nextLink.replace("http://", "");
                nextBaseUrl = nextBaseUrl.replace("https://", "");
                nextBaseUrl = nextBaseUrl.replace("www.", "");
                nextBaseUrl = nextBaseUrl.split("//")[0];
                if (!nextLinksExtern.contains(nextBaseUrl)&&!nextBaseUrl.isEmpty()) {
                    nextLinksExtern.add(nextBaseUrl);
                    //System.out.println("add " + nextBaseUrl);
                }
                
            }
            insertBackLinkIntoMap(nextLink);
        }

        for (String link : nextLinksIntern) {
            if (!urls.contains(link) && !super.isInterrupted()) {
                //coordinator.addToQueue(nextLink);
               
                crawl(link, baseUrl);
            }
        }
        coordinator.addToQueueAll(nextLinksExtern); //toDo überprüfen ob bereits in queue und ob bereits gecrawlt?!

    }

//    private boolean checkIfUrlNeedsToBeCrawled(String url) {
//        for (String mainUrl : urlCache) {
//            if (url.contains(mainUrl)) {
//                return true;
//            }
//        }
//        return false;
//    }
    private String generateFileName(String url) {
        String transformedUrl = url.replace("http://", "");
        transformedUrl = transformedUrl.replace("www.", "");
        transformedUrl = transformedUrl.replace("https://", "");
        transformedUrl = transformedUrl.replace('?', '_');
        //transformedUrl = transformedUrl.replace('/', '_');
        transformedUrl = transformedUrl.replace('\\', '_');
        transformedUrl = transformedUrl.replace('<', '_');
        transformedUrl = transformedUrl.replace('>', '_');
        transformedUrl = transformedUrl.replace('|', '_');
        transformedUrl = transformedUrl.replace('"', '_');
        return coordinator.getDocPath() + transformedUrl + ".txt";
    }

    private void insertBackLinkIntoMap(String nextLink) {
        MutableInt count = backLinks.get(nextLink);
        if (count == null) {
            backLinks.put(nextLink, new MutableInt());
        } else {
            count.increment();
        }
    }
}
