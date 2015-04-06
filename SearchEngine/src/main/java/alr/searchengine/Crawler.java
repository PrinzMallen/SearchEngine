/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package alr.searchengine;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Herbe_000
 */
public interface Crawler{
    
    public void run() throws SQLException, IOException;
    public void setNewUrlCache(List<String> urlCache);
    public void finish();
    public void transmitBacklinksAndAdjustRating(Map<Integer, String> backlinks);
}
