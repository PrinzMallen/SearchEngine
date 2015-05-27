/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.hsmannheim.ss15.alr.searchengine;

import java.io.ByteArrayInputStream;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdfparser.NonSequentialPDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

/**
 *
 * @author Alex
 */
public class PDFParser {

    public String getTextOfPDF(byte[] in) throws Exception {

        ByteArrayInputStream input = new ByteArrayInputStream(in);

        org.apache.pdfbox.pdfparser.PDFParser parser;
        String parsedText = null;;
        PDFTextStripper pdfStripper = null;
        PDDocument pdDoc = null;
        COSDocument cosDoc = null;

        parser = new NonSequentialPDFParser(input);

        //parse PDF
        try {
            parser.parse();
            cosDoc = parser.getDocument();
            pdfStripper = new PDFTextStripper();
            pdDoc = new PDDocument(cosDoc);

            parsedText = pdfStripper.getText(pdDoc);
        } catch (Exception e) {
            throw (e);

        } finally {
            if (cosDoc != null) {
                cosDoc.close();
            }
            if (pdDoc != null) {
                pdDoc.close();
            }

        }
        return parsedText;
    }
}
