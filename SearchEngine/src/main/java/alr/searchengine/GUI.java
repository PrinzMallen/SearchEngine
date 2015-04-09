/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package alr.searchengine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;

/**
 *
 * @author Herbe_000
 */
public class GUI {

    static String indexDir = "target\\index";
    static String docsDir = "target\\files\\";
    private static LuceneController lController = new LuceneController(indexDir, docsDir);
    private static Coordinator coordinator = new Coordinator(docsDir, indexDir);

    public static void main(String[] argu) throws InterruptedException, IOException, ParseException {
        coordinator.addToQueue("http://www.hs-mannheim.de/");
        coordinator.startCrawler(1);
        Thread.sleep(20000);
        coordinator.stopCrawler();
        lController.refreshIndex();
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        System.out.println("Enter query String");
        String line =  in.readLine();
        List<Document> results = lController.doSearchQuery(line);
        for (Document d : results) {
            System.out.println(d.get("path"));
        }
        System.out.println("Found "+results.size()+" matching docs");
    }
}
