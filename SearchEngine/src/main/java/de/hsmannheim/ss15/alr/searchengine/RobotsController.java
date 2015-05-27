package de.hsmannheim.ss15.alr.searchengine;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Alex
 */
public class RobotsController {
     private final Logger LOGGER = LoggerFactory.getLogger(RobotsController.class);
    private List<String> disallowedForRobots = new ArrayList<>();

    
    
    public void evaluateRobotsTxt(URL url) {
        try {
            Connection.Response res = Jsoup.connect(url.toString() + "/robots.txt").ignoreContentType(true).timeout(10 * 100).execute();
            String text = res.body();
            for (String useragent : text.split("(?=User-agent:)")) {
                if (useragent.contains("User-agent: *")) {
                    text = useragent;
                    break;
                }
            }
            String[] splitByNewLine = text.split("\n");
            for (String rule : splitByNewLine) {
                if (rule.contains("Disallow") && rule.contains("/")) {
                    String forbiddenUrl = rule.substring(rule.indexOf("/"), rule.length());
                    forbiddenUrl = url.getProtocol() + "://" + url.getHost() + "" + forbiddenUrl;
                    if (!disallowedForRobots.contains(forbiddenUrl)) {
                        disallowedForRobots.add(forbiddenUrl);
                    }

                }
            }
        } catch (org.jsoup.HttpStatusException e) {
            //ok
        } catch (IOException ex) {
            LOGGER.error("Exception while connecting to " + url);
        }
}
    
     public boolean checkIfUrlIsForbidden(URL url) {
        for (String rule : disallowedForRobots) {
            if (url.toString().contains(rule)) {
                return true;
            }
        }
        return false;
    }
}
