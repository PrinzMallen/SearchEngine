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
    private ConcurrentHashMap crawlerThreads = new ConcurrentHashMap<Integer, DefaultCrawler>();
    private String docPath = "";
    private String indexPath = "";
    private LuceneController lController = new LuceneController(indexPath);
    private ConcurrentLinkedQueue<String> workingQueue;
     private final Logger LOGGER = LoggerFactory.getLogger(Coordinator.class);
    
//---------------------------------------------------------constructors---------------------------------------------------------
    public Coordinator (){
    }
    
    
//---------------------------------------------------------public methods---------------------------------------------------------
    public void startCrawler(int numberOfCrawler){
        for (int i=1; i <= numberOfCrawler; i++){
            crawlerThreads.put(i, new DefaultCrawler(getUrlCacheForCrawler(), this));
            ((DefaultCrawler) crawlerThreads.get(i)).run();
        }
    }
    
    public ConcurrentHashMap<String, MutableInt> getSiteRating(){
        return siteRating;
    }
    
    //Zwei Dinge zu beachten, Zeit seitdem die Seite zuletzt besucht wurde und das Rating. Eventuell einen Faktor daraus bilden den man vergleichen kann.
    public void orderUrlsToCrawl(){
    }
    
    public String getDocPath() {
        return docPath;
    }
    
    
//---------------------------------------------------------private methods---------------------------------------------------------
    private List <String> getUrlCacheForCrawler(){
        List <String> urlCache = new ArrayList<String>();
        for(int i=0; i<crawlerCacheSize; i++){
            urlCache.add(workingQueue.poll()); //poll kann null liefern!
            i++;
        }
        
        return urlCache;
    }
    
    
}
