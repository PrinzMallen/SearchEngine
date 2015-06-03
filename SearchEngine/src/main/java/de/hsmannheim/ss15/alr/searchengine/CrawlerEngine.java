/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.hsmannheim.ss15.alr.searchengine;


import java.net.MalformedURLException;
import java.net.URL;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 *
 * @author Herbe_000
 */
public class CrawlerEngine {

    static String docsDir = System.getProperty("user.home") + "\\SearchEngine\\files\\";
    private static Coordinator coordinator = new DefaultCoordinator(docsDir, 3);
    private static boolean showMenu = true;

    public static void main(String[] argu) throws Exception {
        if (argu.length != 1) {
            System.err.println("Usage: java CrawlerEngine <StartUrl>");
            return;
        }

        setTrustAllCerts();
        try {
            URL url = new URL(argu[0]);
            coordinator.addToQueue(url.toString());
        } catch (MalformedURLException ex) {
            System.err.println("Your start url was malformed");
            return;
        }

        docsDir = System.getProperty("user.home") + "\\SearchEngine\\files";
        System.out.println("The files that have been crawled will be saved at: " + docsDir);
        coordinator.startCrawler();

    }

    private static void setTrustAllCerts() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
        };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(
                    new HostnameVerifier() {
                        public boolean verify(String urlHostName, SSLSession session) {
                            return true;
                        }
                    });
        } catch (Exception e) {
            //We can not recover from this exception.
            e.printStackTrace();
        }
    }
}
