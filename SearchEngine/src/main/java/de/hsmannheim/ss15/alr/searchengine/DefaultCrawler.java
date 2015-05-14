/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.hsmannheim.ss15.alr.searchengine;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import static java.lang.Thread.interrupted;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import javax.net.ssl.SSLHandshakeException;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import org.jsoup.Connection;

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
        //crawl as long as urls are available. If every url was crawled get some new from coordinator
        while (!interrupted()) {
            while (!urlCache.isEmpty() && !interrupted()) {
                if (!interrupted()) {
                    String element = urlCache.remove(0);
                    try {
                        URL url = new URL(element);
                        crawl(url);
                    } catch (MalformedURLException ex) {
                        LOGGER.error("MalformedURL: " + element, ex);
                    }

                }

            }
            urlCache = coordinator.getUrlCacheForCrawler();
            urls.clear();
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
            // if entry did not exist create one with value 1
            if (count == null) {
                siteRating.put(backLink, new MutableInt());
            } //if entry did exist, increment value 
            else {
                count.incrementBy(backLinks.get(backLink).get());
            }
        }
    }

    @Override
    public void setNewUrlCache(List<String> urlCache) {
        this.urlCache = urlCache;
    }

//---------------------------------------------------------private methods---------------------------------------------------------
    private void crawl(URL url) {
        //make sure the directory for files does exist
        String docPath = coordinator.getDocPath();
        if (!new File(docPath).exists()) {
            new File(docPath).mkdir();
        }

        Document doc = null;
        String contentType = null;
        long start = System.currentTimeMillis();
        Connection.Response res = null;
        //get response from page
        try {
            res = Jsoup.connect(url.toString()).ignoreContentType(true).ignoreHttpErrors(true).timeout(10 * 100).execute();
            doc = res.parse();
            contentType = res.contentType();

        } catch (SocketTimeoutException | java.net.ConnectException ex) {
            //try again
            urlCache.add(url.toString());
            return;
        } catch (SSLHandshakeException ex) {
            LOGGER.error("SSL Exception for " + url, ex);
            urls.add(url.toString());
            return;
        } catch (Throwable t) {
            LOGGER.error("Exception while connecting to " + url, t);
            urls.add(url.toString());
            return;

        }
        LOGGER.debug("getting doc " + (System.currentTimeMillis() - start));
        docCounter++;
        //in case the contentType is text, just save the file
        if (contentType.contains("text")) {
            try {
                saveFileWithText(url.toString(), doc.text(),false);

            } catch (IOException e) {
                LOGGER.error("Exception while saving text: " + url.toString(), e);
            }
            evaluateLinksOnPage(doc, url);

        } // in case the contentTyp is pdf
        else if (contentType.contains("pdf")) {
            try {
                String parsedText = getTextOfPDF(res);
                saveFileWithText(url.toString(), parsedText,true);
            } catch (IOException ex) {
                LOGGER.error("Exception while parsing PDF: " + url.toString(), ex);
            }

        } else {
            LOGGER.debug("wrong content type " + url);
        }
        urls.add(url.toString());

    }

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
        transformedUrl = transformedUrl.replace(':', '_');
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

    public int getDocCounter() {
        return docCounter;
    }

    private void saveFileWithText(String url, String text, boolean pdf) throws IOException {
        //generate fileName that is ok for windows (e.g. no ':')
        String fileName = generateFileName(url.toString());
        String name;
        File file = new File(fileName);
        //make sure directories do exist. Each Level of the Url will be a new directory
        if (fileName.contains("/")) {
            int last = fileName.lastIndexOf("/");
            name = fileName.substring(0, last);
            File directory = new File(name);
            directory.mkdirs();
        }
        //write only text into file ( no html tags etc.)
        try (BufferedWriter output = new BufferedWriter(new FileWriter(file))) {
            output.write("URL:" + url);
            output.newLine();
            if (pdf) {
                output.write("DataType:PDF");
            } else {
                output.write("DataType:TEXT");
            }
            output.newLine();

            output.write(text);
        }

    }

    private void evaluateLinksOnPage(Document doc, URL url) {
        //get all links of the page. Its needed to divide into links which point to a page of the same host 
        //and links which point to a other host
        Elements questions = doc.select("a[href]");
        List<String> nextLinksIntern = new ArrayList<>();
        List<String> nextLinksExtern = new ArrayList<>();
        for (Element link : questions) {
            //get value of html tag
            String nextLink = link.attr("abs:href");
            int lvlCount = nextLink.length() - nextLink.replace("//", "").length();
            //if there is a maximum level for a url ( -1 means no max), check if in range
            if (maxLvl != -1 && lvlCount > maxLvl) {
                break;
            }
            //just in case 
            if (nextLink.isEmpty()) {
                break;
            }
            //if the new link contains the host of actual link, its considered as intern
            if (nextLink.contains(url.getHost())) {
                if (!nextLinksIntern.contains(nextLink)) {
                    nextLinksIntern.add(nextLink);
                }

            } //if not its a new host and considered as extern
            else {
                URL newUrl;
                try {
                    newUrl = new URL(nextLink);
                    String nextBaseUrl = newUrl.getHost();
                    if (!nextLinksExtern.contains(nextBaseUrl) && !nextBaseUrl.isEmpty()) {
                        nextLinksExtern.add(newUrl.getProtocol() + "://" + nextBaseUrl);
                    }
                    insertBackLinkIntoMap(nextBaseUrl);
                } catch (MalformedURLException ex) {
                    LOGGER.error("MalformedURL: " + nextLink, ex);
                }

            }
        }

        //each Intern link will be handled by this crawler
        for (String link : nextLinksIntern) {
            if (!urls.contains(link) && !super.isInterrupted()) {

                urlCache.add(link);

            }
        }
        //each extern link will be send to the coordinator
        coordinator.addToQueueAll(nextLinksExtern); //toDo überprüfen ob bereits in queue und ob bereits gecrawlt?!

    }

    private String getTextOfPDF(Connection.Response res) throws IOException {
        //get response as byteStream
        ByteArrayInputStream is = new ByteArrayInputStream(res.bodyAsBytes());
        PDFParser parser;
        String parsedText = null;;
        PDFTextStripper pdfStripper = null;
        PDDocument pdDoc = null;
        COSDocument cosDoc = null;

        parser = new PDFParser(is);

        //parse PDF
        try {
            parser.parse();
            cosDoc = parser.getDocument();
            pdfStripper = new PDFTextStripper();
            pdDoc = new PDDocument(cosDoc);
            pdfStripper.setStartPage(1);
            pdfStripper.setEndPage(10);
            parsedText = pdfStripper.getText(pdDoc);
        } catch (Exception e) {
            LOGGER.error("An exception occured in parsing the PDF Document", e);

        } finally {
            if (cosDoc != null) {
                cosDoc.close();
            }
            if (pdDoc != null) {
                pdDoc.close();
            }

        }
        return parsedText;
    }

}
