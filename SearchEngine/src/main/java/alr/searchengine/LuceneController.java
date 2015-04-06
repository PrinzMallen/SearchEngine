/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package alr.searchengine;

import java.util.List;
import org.apache.lucene.document.Document;

/**
 *
 * @author Herbe_000
 */
public class LuceneController {

//---------------------------------------------------------object attributes---------------------------------------------------------
    private String indexDir;

    
//---------------------------------------------------------constructors---------------------------------------------------------
    public LuceneController(String indexDir){
        this.indexDir = indexDir;
    }
    
    
//---------------------------------------------------------public methods---------------------------------------------------------
    public void refreshIndex(){
    
    }
    
    public List<Document> doSearchQuery(String query){
        return null;
    }
    
    
//---------------------------------------------------------private methods---------------------------------------------------------
}
