/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.hsmannheim.ss15.alr.searchengine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Herbe_000
 */
public class DefaultCoordinator extends Coordinator {

//---------------------------------------------------------object attributes---------------------------------------------------------
    private ConcurrentHashMap<String, MutableInt> siteRating;

//---------------------------------------------------------constructors---------------------------------------------------------
    public DefaultCoordinator(String docPath, int crawlerCacheSize) {
        this.docPath = docPath;
        this.crawlerCacheSize = crawlerCacheSize;
        LOGGER = LoggerFactory.getLogger(DefaultCoordinator.class);
        workingQueue = new ConcurrentLinkedQueue<>();
        siteRating = new ConcurrentHashMap<>();
        crawlerThreads = new CopyOnWriteArrayList<>();
        new Timer().schedule(new TimerTask() {

            @Override
            public void run() {
                orderUrlsToCrawl();
            }
        }, 20000, 300000);
    }

    public DefaultCoordinator() {
    }

//---------------------------------------------------------public methods---------------------------------------------------------
    @Override
    public void startCrawler() {

        DefaultCrawler crawler = new DefaultCrawler(getNextUrls(), this, "crawler" + crawlerThreads.size());
        crawlerThreads.add(crawler);
        crawler.start();
        LOGGER.info("New Crawler started. There are now " + crawlerThreads.size() + " active Crawlers");

    }

    public ConcurrentHashMap<String, MutableInt> getSiteRating() {
        return siteRating;
    }

    public void orderUrlsToCrawl() {
        long start=System.currentTimeMillis();
         try{
        List mapKeys = new ArrayList(siteRating.keySet());
        List mapValues = new ArrayList(siteRating.values());
        Collections.sort(mapValues,new Comparator() {

            @Override
            public int compare(Object o1, Object o2) {
                Integer i1=((MutableInt)o1).get();
                Integer i2=((MutableInt)o2).get();
                return i2.compareTo(i1);
            }
        });
        Collections.sort(mapKeys) ;
          LinkedHashMap<String,MutableInt> sortedMap = new LinkedHashMap<String,MutableInt> ();
       
      

        Iterator<MutableInt> valueIt = mapValues.iterator();
        while (valueIt.hasNext()) {
            MutableInt val = valueIt.next();
            Iterator<String> keyIt = mapKeys.iterator();
           
            while (keyIt.hasNext()) {
                String key = keyIt.next();
                MutableInt comp1 = siteRating.get(key);
                MutableInt comp2 = val;
                if (comp1.get()==comp2.get()) {
                    siteRating.remove(key);
                    mapKeys.remove(key);
                    sortedMap.put(key, val);
                    break;
                }

            }

        }
        String[] temp=new String[workingQueue.size()];
        temp=workingQueue.toArray(temp);
        workingQueue=new ConcurrentLinkedQueue<>();
        for(String s:sortedMap.keySet()){
            workingQueue.offer(s);
        }
        for(String s:temp){
            if(!workingQueue.contains(s)){
                workingQueue.offer(s);
            }
        }
      }
        catch(Throwable t){
            LOGGER.error("Error while added rated sites", t);
        }
         siteRating.clear();
}

public String getDocPath() {
        return docPath;
    }

    public void addToQueue(String url) {

        if (!workingQueue.contains(url)) {
            workingQueue.offer(url);
        }

        if (checkIfNewCrawlerhasToStart()) {
            startCrawler();
        }

    }

    public void adAlldToQueue(List<String> urls) {
        for (String url : urls) {
            if (!workingQueue.contains(url)) {
                workingQueue.offer(url);
            }
        }

        if (checkIfNewCrawlerhasToStart()) {
            startCrawler();
        }
    }

//---------------------------------------------------------private methods---------------------------------------------------------
    public List<String> getNextUrls() {
        List<String> urlCache = new ArrayList<String>();
        for (int i = 0; i < crawlerCacheSize; i++) {
            if (!workingQueue.isEmpty()) {
                String newUrl = workingQueue.poll();
                urlCache.add(newUrl); //poll kann null liefern!
                i++;
                LOGGER.info("Crawler took new root URL: " + newUrl);
            }
        }

        return urlCache;
    }

    private boolean checkIfNewCrawlerhasToStart() {
        int queueSize = workingQueue.size();
        int crawlerCount = crawlerThreads.size();

        if (crawlerCount != 0 && queueSize / crawlerCount > 20 && crawlerCount < 50) {
            return true;
        } else {
            return false;
        }
    }

}
