/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package alr.searchengine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author Herbe_000
 */
public class DefaultCrawler implements Crawler, Runnable{

//---------------------------------------------------------object attributes---------------------------------------------------------
    private List<String> urlCache;
    private Coordinator coordinator;
    private boolean[] params;
    private int docCounter = 0;
    private String docName = "";
    private Map<String,String> urls=new HashMap<String, String>();

    
//---------------------------------------------------------constructors---------------------------------------------------------
    public DefaultCrawler (List <String> urlCache, Coordinator coordinator){
        this.urlCache = urlCache;
        this.coordinator = coordinator;
    }
    
    public DefaultCrawler (List <String> urlCache, Coordinator coordinator, boolean[] params){
        this.urlCache = urlCache;
        this.coordinator = coordinator;
        this.params = params;
    }

    
//---------------------------------------------------------public methods---------------------------------------------------------
    @Override
    public void run() {
        for (String element : urlCache) {
            try {
                crawl(element);
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public void finish() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void transmitBacklinksAndAdjustRating(Map<Integer, String> backlinks) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setNewUrlCache(List<String> urlCache) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
//---------------------------------------------------------private methods---------------------------------------------------------
    private void crawl(String url) throws SQLException, IOException {
        String docPath = coordinator.getDocPath();
        if(!new File(docPath).exists()){
            new File(docPath).mkdir();
        }

        //get useful information
        Document doc = Jsoup.connect(url).timeout(10 * 1000).get();

        Writer writer = null;

        docCounter++;
        //save as text as txt
        try {
            File file = new File(docPath + docName + docCounter + ".txt");
            BufferedWriter output = new BufferedWriter(new FileWriter(file));
            output.write(doc.text());
            output.close();
            urls.put(url, docCounter+"");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //get all links and recursively call the processPage method
        Elements questions = doc.select("a[href]");
        for (Element link : questions) {

            if(!urls.containsKey(link.attr("abs:href"))){
                crawl(link.attr("abs:href"));
            }

        }
    }
}


