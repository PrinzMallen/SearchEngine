/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.hsmannheim.ss15.alr.searchengine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
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

import org.jsoup.Connection;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.slf4j.LoggerFactory;

/**
 *
 * @author Herbe_000
 */
public class DefaultCrawler extends Crawler {

//---------------------------------------------------------object attributes---------------------------------------------------------
    private Map<String, MutableInt> backLinks = new HashMap<>();
    
//---------------------------------------------------------constructors---------------------------------------------------------
    public DefaultCrawler(List<String> urlCache, DefaultCoordinator coordinator, String name) {
        this.urlCache = urlCache;
        this.coordinator = coordinator;
        this.name = name;
        LOGGER = LoggerFactory.getLogger(DefaultCrawler.class);
        robotsController = new RobotsController();
        pdfParser = new de.hsmannheim.ss15.alr.searchengine.PDFParser();
    }

//---------------------------------------------------------public methods---------------------------------------------------------
    @Override
    public void run() {
        File urlstore=new File(System.getProperty("user.home") + "\\SearchEngine\\urlStore");
        urlstore.mkdirs();
        //crawl as long as urls are available. If every url was crawled get some new from coordinator
        while (!interrupted()) {
            while (!urlCache.isEmpty() && !interrupted()) {
                if (!interrupted()) {
                    String element = urlCache.remove(0);
                    try {
                        URL url = new URL(element);
                        crawl(url);
                        if (urlCache.isEmpty()) {
                            deserialiseList(250);
                            transmitBacklinksAndAdjustRating();
                        
                        }

                    } catch (MalformedURLException ex) {
                        LOGGER.error("MalformedURL: " + element, ex);
                    }
                }

            }
           
            urlCache = coordinator.getNextUrls();
        }
    }

    public void finish() {

    }

    private void transmitBacklinksAndAdjustRating() {
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
        backLinks.clear();
    }

    @Override
    public void setNewUrlCache(List<String> urlCache) {
        this.urlCache = urlCache;
    }

//---------------------------------------------------------private methods---------------------------------------------------------
    @Override
    public void crawl(URL url) {
        //for the moment crawl every page only once. Later there can be an update interval

        if (new File(generateFileName(url.toString())).exists() && !isRootUrl(url)) {
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
            robotsController.evaluateRobotsTxt(url);
        }

        if (robotsController.checkIfUrlIsForbidden(url)) {
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
            LOGGER.warn("could not connect to " + url + " . Reason: " + ex.getMessage());
            return;
        } catch (SSLHandshakeException ex) {
            LOGGER.error("SSL Exception for " + url, ex);
            return;
        } catch (Throwable t) {
            LOGGER.error("could not connect to " + url);

            return;

        }
        LOGGER.debug("getting doc " + (System.currentTimeMillis() - start) + " " + url.toString());
        //in case the contentType is text, just save the file

        if (contentType == null) {
            return;
        }
        if (contentType.contains("text")) {
            try {
                String text = "URL:" + url.toString() + "\r\n";
                text += "DataType:TEXT\r\n";
                text += "Title:" + doc.title() + "\r\n";
                text += doc.text();
                saveFileWithText(text,url);

            } catch (IOException e) {
                LOGGER.error("Exception while saving text: " + url.toString(), e);
            }
            evaluateLinksOnPage(doc, url);

        } // in case the contentTyp is pdf
        else if (contentType.contains("pdf")) {
            try {
                String parsedText = pdfParser.getTextOfPDF(res.bodyAsBytes());
                String text = "URL:" + url.toString() + "\r\n";
                text += "DataType:PDF\r\n";
                text += "Title:" + doc.title() + "\r\n";
                text += parsedText;
                saveFileWithText(text,url);
            } catch (Exception ex) {
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
        String completePath = coordinator.getDocPath() + transformedUrl + "/" + lastName;
        if (completePath.length() > 250) {
            completePath = completePath.substring(0, 250);
        }
        return completePath + ".txt";
    }

    private void insertBackLinkIntoMap(String nextLink) {
        MutableInt count = backLinks.get(nextLink);
        if (count == null) {
            backLinks.put(nextLink, new MutableInt());
        } else {
            count.increment();
        }
    }

    private void saveFileWithText(String text,URL url) throws IOException {
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
        try (BufferedWriter output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),"UTF-8"))) {         
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

            //just in case 
            if (nextLink.isEmpty()) {
                break;
            }
            //if the new link contains the host of actual link, its considered as intern
            if (nextLink.contains(url.getHost())) {
                if (!nextLinksIntern.contains(nextLink) && !new File(generateFileName(nextLink)).exists()) {
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
                        insertBackLinkIntoMap(nextBaseUrl);
                    }

                } catch (MalformedURLException ex) {
                    LOGGER.error("MalformedURL: " + nextLink, ex);
                }

            }
        }
        serialiseList(nextLinksIntern, true);
        //each Intern link will be handled by this crawler
//        for (String link : nextLinksIntern) {
//            if (!super.isInterrupted()) {
//
//                urlCache.add(link);
//
//            }
//        }
        //each extern link will be send to the coordinator
        coordinator.adAlldToQueue(nextLinksExtern); //toDo überprüfen ob bereits in queue und ob bereits gecrawlt?!

    }

    private void serialiseList(List<String> list, boolean append) {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(System.getProperty("user.home") + "\\SearchEngine\\urlstore\\"+name + ".txt", append)))) {
            for (String s : list) {
                out.println(s);
            }
            out.close();

        } catch (IOException e) {
            //exception handling left as an exercise for the reader
        }
    }

    private void deserialiseList(int count) {
        try (BufferedReader in = new BufferedReader(new FileReader(new File(System.getProperty("user.home") + "\\SearchEngine\\urlstore\\"+name + ".txt")))) {
            List<String> list = new ArrayList<>();
            while (in.ready()) {
                list.add(in.readLine());
            }
            List<String> listToadd = new ArrayList<>();
            for (int i = 0; i < count && i < list.size(); i++) {
                listToadd.add(list.remove(0));
            }

            urlCache.addAll(listToadd);
            serialiseList(list, false);

        } catch (IOException e) {
            //exception handling left as an exercise for the reader
        }
    }

    private boolean isRootUrl(URL url) {
        return (url.toString().equals(url.getProtocol() + "://" + url.getHost()) || (url.toString().equals(url.getProtocol() + "://" + url.getHost() + "/")));
    }

}
