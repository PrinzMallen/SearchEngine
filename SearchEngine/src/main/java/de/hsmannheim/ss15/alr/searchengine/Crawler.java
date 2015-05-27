/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.hsmannheim.ss15.alr.searchengine;

import java.net.URL;
import java.util.List;
import org.slf4j.Logger;

/**
 *
 * @author Herbe_000
 */
public abstract class Crawler extends Thread {

    protected DefaultCoordinator coordinator;
    protected List<String> urlCache;
    protected RobotsController robotsController;
    protected String name;
    protected Logger LOGGER;
    protected PDFParser pdfParser;

    
    /**
     * this method has to set some new urls to crawl.
     * @param urlCache list of new urls
     */
    public abstract void setNewUrlCache(List<String> urlCache);
    
    /**
     * this method has to crawl the websites
     * @param url 
     */
    public abstract void crawl(URL url);
    
   

   
}
