/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package alr.searchengine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Herbe_000
 */
public class Coordinator {

//---------------------------------------------------------object attributes---------------------------------------------------------
    private int crawlerCacheSize = 1;
    private ConcurrentHashMap siteRating = new ConcurrentHashMap<String, MutableInt>();
    private CopyOnWriteArrayList<DefaultCrawler> crawlerThreads = new CopyOnWriteArrayList<>();
    private String docPath = "";
    private String indexPath = "";
    private LuceneController lController = new LuceneController(indexPath, docPath);
    private ConcurrentLinkedQueue<String> workingQueue = new ConcurrentLinkedQueue<>();
    private final Logger LOGGER = LoggerFactory.getLogger(Coordinator.class);
    private AtomicBoolean stopCreatingCrawlers = new AtomicBoolean(false);
    

//---------------------------------------------------------constructors---------------------------------------------------------
    public Coordinator(String docPath, String indexPath) {
        this.docPath = docPath;
        this.indexPath = indexPath;
    }
    
    public Coordinator() {
    }

//---------------------------------------------------------public methods---------------------------------------------------------
    public void startCrawler(int numberOfCrawler) {
        for (int i = 1; i <= numberOfCrawler; i++) {
            DefaultCrawler crawler = new DefaultCrawler(getUrlCacheForCrawler(), this,-1);
            crawlerThreads.add(crawler);
            crawler.start();
        }
        
    }
    
    public ConcurrentHashMap<String, MutableInt> getSiteRating() {
        return siteRating;
    }

    //Zwei Dinge zu beachten, Zeit seitdem die Seite zuletzt besucht wurde und das Rating. Eventuell einen Faktor daraus bilden den man vergleichen kann.
    public void orderUrlsToCrawl() {
    }
    
    public void stopCrawler() {
        
        stopCreatingCrawlers.set(true);
        System.out.println("crawler crating stoped");
        for (Crawler c : crawlerThreads) {
            ((DefaultCrawler) c).interrupt();
            
        }
    }
    
    public String getDocPath() {
        return docPath;
    }
    
    public void addToQueue(String url) {
        
        if (!workingQueue.contains(url)) {
            //LOGGER.info("added " + url + " to the queue");
            workingQueue.offer(url);
        }
        LOGGER.info("Length " + workingQueue.size());
 
    }
    
    public void addToQueueAll(List<String> urls) { 
        workingQueue.addAll(urls);    
    }

//---------------------------------------------------------private methods---------------------------------------------------------
    public List<String> getUrlCacheForCrawler() {
        List<String> urlCache = new ArrayList<String>();
        for (int i = 0; i < crawlerCacheSize; i++) {
            if (!workingQueue.isEmpty()) {
                urlCache.add(workingQueue.poll()); //poll kann null liefern!
                i++;
            }
        }
        
        return urlCache;
    }
    
}
