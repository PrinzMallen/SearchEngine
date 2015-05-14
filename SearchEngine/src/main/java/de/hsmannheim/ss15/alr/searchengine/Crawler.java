/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.hsmannheim.ss15.alr.searchengine;

import java.util.List;
import java.util.Map;

/**
 *
 * @author Herbe_000
 */
public interface Crawler extends Runnable{
    
 
    public void setNewUrlCache(List<String> urlCache);
    public void finish();
    public void transmitBacklinksAndAdjustRating();
}
