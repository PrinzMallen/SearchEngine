/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.hsmannheim.ss15.alr.searchengine;

/**
 *
 * @author Alex
 */
class MutableInt {

    private int value = 1;

    public void increment() {
        ++value;
    }

    public int get() {
        return value;
    }
    
    public void incrementBy(int value){
        this.value+=value;
    }
}

   
        
