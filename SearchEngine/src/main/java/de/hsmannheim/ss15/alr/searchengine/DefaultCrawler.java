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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import javax.net.ssl.SSLHandshakeException;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdfparser.NonSequentialPDFParser;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

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

    private Map<String, MutableInt> backLinks = new HashMap<>();
    private final Logger LOGGER = LoggerFactory.getLogger(DefaultCrawler.class);
    private int maxLvl;
    private List<String> disallowedForRobots = new ArrayList<>();

//---------------------------------------------------------constructors---------------------------------------------------------
    public DefaultCrawler(List<String> urlCache, Coordinator coordinator, int maxLvl) {
        this.urlCache = urlCache;
        this.coordinator = coordinator;
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
        //for the moment crawl every page only once. Later there can be an update interval
        if (new File(generateFileName(url.toString())).exists()&&!isRootUrl(url)) {
            return;
        }
        //make sure the directory for files does exist
        String docPath = coordinator.getDocPath();
        if (!new File(docPath).exists()) {
            new File(docPath).mkdir();
        }
        //not possible or nessesary
        if (url.toString().contains("jsessionid") || url.toString().contains("#")) {
            return;
        }
        //if rootpage check for robots.txt
        if (isRootUrl(url)) {
            evaluateRobotsTxt(url);

        }

        if (checkIfUrlIsForbidden(url)) {
            LOGGER.debug("url is forbitten " + url.toString());
            return;
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

            return;
        } catch (SSLHandshakeException ex) {
            LOGGER.error("SSL Exception for " + url, ex);
            return;
        } catch (Throwable t) {
            LOGGER.error("could not connect to " + url);

            return;

        }
        LOGGER.debug("getting doc " + (System.currentTimeMillis() - start) + " " + url.toString());
        docCounter++;
        //in case the contentType is text, just save the file

        if (contentType == null) {
            return;
        }
        if (contentType.contains("text")) {
            try {
                saveFileWithText(url.toString(), doc.text(), false);

            } catch (IOException e) {
                LOGGER.error("Exception while saving text: " + url.toString(), e);
            }
            evaluateLinksOnPage(doc, url);

        } // in case the contentTyp is pdf
        else if (contentType.contains("pdf")) {
            try {
                String parsedText = getTextOfPDF(res);
                saveFileWithText(url.toString(), parsedText, true);
            } catch (IOException ex) {
                LOGGER.error("Exception while parsing PDF: " + url.toString(), ex);
            }

        } else {
            LOGGER.debug("wrong content type " + url);
        }

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
        String lastName = "";
        if (transformedUrl.contains("/")) {
            lastName = transformedUrl.substring(transformedUrl.lastIndexOf("/") + 1, transformedUrl.length());
            transformedUrl = transformedUrl.substring(0, transformedUrl.lastIndexOf("/") + 1);
            if (lastName.length() > 51) {
                lastName = lastName.substring(0, 50);
            }
        }
        return coordinator.getDocPath() + transformedUrl + "/" + lastName + ".txt";
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
        //do nothing when text is empty
        if (text.isEmpty()) {
            return;
        }

        //generate fileName that is ok for windows (e.g. no ':')
        String fileName = generateFileName(url.toString());
        String name;
        //make sure directories do exist. Each Level of the Url will be a new directory

        if (fileName.contains("/")) {
            int last = fileName.lastIndexOf("/");
            name = fileName.substring(0, last);

            File directory = new File(name);
            //if file name is to long 

            Files.createDirectories(directory.toPath());
        }

        File file = new File(fileName);
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
            if (!super.isInterrupted()) {

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

        parser = new NonSequentialPDFParser(is);

        //parse PDF
        try {
            parser.parse();
            cosDoc = parser.getDocument();
            pdfStripper = new PDFTextStripper();
            pdDoc = new PDDocument(cosDoc);

            parsedText = pdfStripper.getText(pdDoc);
        } catch (Exception e) {
            LOGGER.error("An exception occured in parsing the PDF Document" + res.url().toString(), e);

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

    private boolean checkIfUrlIsForbidden(URL url) {
        for (String rule : disallowedForRobots) {
            if (url.toString().contains(rule)) {
                return true;
            }
        }
        return false;
    }

    private void evaluateRobotsTxt(URL url) {
        try {
            Connection.Response res = Jsoup.connect(url.toString() + "/robots.txt").ignoreContentType(true).timeout(10 * 100).execute();
            String text = res.body();
            for (String useragent : text.split("(?=User-agent:)")) {
                if (useragent.contains("User-agent: *")) {
                    text = useragent;
                    break;
                }
            }
            String[] splitByNewLine = text.split("\n");
            for (String rule : splitByNewLine) {
                if (rule.contains("Disallow") && rule.contains("/")) {
                    String forbiddenUrl = rule.substring(rule.indexOf("/"), rule.length());
                    forbiddenUrl = url.getProtocol() + "://" + url.getHost() + "" + forbiddenUrl;
                    if (!disallowedForRobots.contains(forbiddenUrl)) {
                        disallowedForRobots.add(forbiddenUrl);
                    }

                }
            }
        } catch (org.jsoup.HttpStatusException e) {
            //ok
        } catch (IOException ex) {
            LOGGER.error("Exception while connecting to " + url);
        }
    }

    private boolean isRootUrl(URL url) {
        return (url.toString().equals(url.getProtocol() + "://" + url.getHost()) || (url.toString().equals(url.getProtocol() + "://" + url.getHost() + "/")));
    }

}
