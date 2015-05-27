/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.hsmannheim.ss15.alr.searchengine;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;

/**
 *
 * @author Alex
 */
public abstract class Coordinator {

    protected int crawlerCacheSize;
    protected CopyOnWriteArrayList<Crawler> crawlerThreads;
    protected String docPath;
    protected ConcurrentLinkedQueue<String> workingQueue;
    protected Logger LOGGER ;
    
    /**
     * this method starts a new Crawler with some starting urls
     */
    public abstract void startCrawler();
    /**
     * this method adds a new url to the Queue ( if the url isnt already in the queue)
     * @param url the url to add
     */
    public abstract void addToQueue(String url);
    /**
     * this methods adds new urls to the Queue
     * @param urls the list of urls to add
     */
    public abstract void adAlldToQueue(List<String> urls);
    /**
     * this method returns some urls from the queue
     * @return some urls form the queue
     */
    public abstract List<String> getNextUrls();
    
   
  
    


    
}
