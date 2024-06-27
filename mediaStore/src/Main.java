import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Klasse mit der die Parser für alle Files in Data ausgeführt werden
 */
public class Main {
    //Pfade zu den Dateien, die Daten enthalten ´, die geladen werden sollen
    private static final String FILENAME[] = {"Data/dresden.xml", "Data/leipzig_transformed.xml", "Data/categories.xml", "Data/reviews.csv"};

    /**
     * Main Methode, es wird jeder Dateipfad behandelt, abgefragt welches Format dieser besitzt und der zugehörige Parser aufgerufen.
     * Dadurch werden alle Daten in die Datenbank geladen.
     * Außerdem wird ein Filewriter zum schreiben der Errors generiert und auch wieder geschlossen.
     * Es wird ebenfalls über den Filewriter alle ErrorCounter aufelistet.
     * @param args
     * @throws IOException
     */

    public static void main(String[] args) throws IOException {

        // Instantiate the Factory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        File errorLog = new File("errorLog.txt");
        FileWriter logWriter = new FileWriter("errorLog.txt");


        try {
            ArrayList<ArrayList<Integer>> errorCounters = new ArrayList<>();
            for (String s : FILENAME) {
                if (s.contains(".xml")) {
                    // parse XML file
                    DocumentBuilder db = dbf.newDocumentBuilder();

                    Document doc = db.parse(s);

                    // optional, but recommended
                    doc.getDocumentElement().normalize();

                    Element root = doc.getDocumentElement();
                    if (root.getNodeName().equals("shop")) {
                       shopParser shopParser = new shopParser(s, logWriter);
                       errorCounters.add(shopParser.getErrors());
                    } else if (root.getNodeName().equals("categories")) {
                        categoryParser categoryParser = new categoryParser(s, logWriter);
                        errorCounters.add(categoryParser.getErrors());
                    }
                } else if (s.contains(".csv")) {
                    ReviewParser reviewParser = new ReviewParser(s, logWriter);
                    errorCounters.add(reviewParser.getErrors());
                }
            }
            try{
                int unknownPgroup = 0, noAsin =0, noTitle =0, itemAlreadyinDB =0, categoryProductNotInDB = 0, reviewProductNotInDB = 0, reviewRatingOutOfRange= 0;
                for(ArrayList<Integer> errors:errorCounters){
                    if(errors.size() == 4){
                        unknownPgroup = unknownPgroup + errors.get(0);
                        noAsin = noAsin + errors.get(1);
                        noTitle = noTitle + errors.get(2);
                        itemAlreadyinDB = itemAlreadyinDB + errors.get(3);
                    }
                    if(errors.size() == 1){
                        categoryProductNotInDB = categoryProductNotInDB + errors.get(0);
                    }
                    if(errors.size() == 2){
                        reviewProductNotInDB = reviewProductNotInDB + errors.get(0);
                        reviewRatingOutOfRange = reviewRatingOutOfRange + errors.get(1);
                    }
                }
                logWriter.write("------ Counter of the Errors ------\n" +
                        "Unbekannter Typ : "+ unknownPgroup+ "\n" +
                        "Keine Asin : " + noAsin + "\n" +
                        "Kein Titel : " + noTitle + "\n" +
                        "Produkt schon in DB : " + itemAlreadyinDB+ "\n" +
                        "Produkt von Kategorie nicht in DB : " + categoryProductNotInDB + "\n" +
                        "Produkt von Review nicht in DB : " + reviewProductNotInDB + "\n" +
                        "Rating von Review nicht valide : " + reviewRatingOutOfRange);
            }catch (IOException e){
                e.printStackTrace();
            }
            logWriter.close();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }
}
