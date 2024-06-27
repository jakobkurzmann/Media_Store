import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;

/**
 * Klasse um XML Dokumente von Shops zu parsen.
 * Fügt alle Vorhandenen Produkte, sowie ÄhnlicheProdukte und Konditionen in die Datenbank ein
 * Schreibt dabei über den ErrorWriter Errors, die beim Parsen des Dokuments- und Laden der Produkte auftreten
 */
public class shopParser {
    private final FileWriter errorWriter;
    int unknownPgroup = 0, noAsin = 0, noTitle = 0, itemAlreadyinDB = 0;

    /**
     * Construkter in dem eine Verbindung zur Datenbank aufgebaut wird, ein Tupel Filliale in die Datenbank geladen wird und die Methoden zum Parsen
     * des Dokuments aufgerufen werden
     *
     * @param fileName    Dateipfad zur Datei die geparsed werden soll
     * @param errorWriter Ein FileWriter der die Errors schreibt
     */
    public shopParser(String fileName, FileWriter errorWriter) {
        this.errorWriter = errorWriter;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try (Connection conn = Verbindung.connect()) {
            try {
                DocumentBuilder db = dbf.newDocumentBuilder();

                Document doc = db.parse(new File(fileName));

                doc.getDocumentElement().normalize();

                Element root = doc.getDocumentElement();

                String plz = root.getAttribute("zip");
                String straße = root.getAttribute("street");
                String name = root.getAttribute("name");
                int shopID = loadShop(plz, straße, name, conn);
                addAllItems(root, shopID, conn);
                addAllSimilars(root, conn);
            } catch (ParserConfigurationException | SAXException | IOException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Methode in der Komplett durch das Dokument geparsed wird und dabei jedes valide Produkt extrahiert und in die Datenbank geladen wird.
     * Für invalide Produkte wird dabei ein Error geschrieben.
     * Bei einer vorhandenen Kondition eines Produkts wird diese ebenfalls in die Datenbank geladen.
     *
     * @param shopElement Wurzel Element des Dokumentes über das geparsed wird
     * @param filial_id   Fillial-ID als Fremdschlüssel für die Konditionen
     * @param conn        Verbindung zur Datenbank
     */
    public void addAllItems(Element shopElement, int filial_id, Connection conn) {
        NodeList children = shopElement.getChildNodes();
        for (int temp = 0; temp < children.getLength(); temp++) {
            Node nNode = children.item(temp);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element itemElement = (Element) nNode;
                if (!Item.checkItem(itemElement.getAttribute("asin"), conn)) {
                    switch (itemElement.getAttribute("pgroup").toLowerCase()) {
                        case "dvd" -> {
                            DVD dvd = new DVD(itemElement);
                            if (dvd.getErrors().size() > 0) {

                                writeErrorsOfItem(dvd);
                            } else {
                                System.out.println("dvd " + itemElement.getAttribute("asin"));
                                DVD.loadDVD(dvd, conn);
                                addItemCondition(itemElement, filial_id, conn);
                            }
                        }
                        case "book" -> {
                            Book buch = new Book(itemElement);
                            if (buch.getErrors().size() > 0) {
                                writeErrorsOfItem(buch);
                            } else {
                                System.out.println("buch" + itemElement.getAttribute("asin"));
                                Book.loadBook(buch, conn);
                                addItemCondition(itemElement, filial_id, conn);
                            }
                        }
                        case "music" -> {
                            Music music = new Music(itemElement);
                            if (music.getErrors().size() > 0) {
                                writeErrorsOfItem(music);
                            } else {
                                System.out.println("music " + itemElement.getAttribute("asin"));
                                Music.loadMusic(music, conn);
                                addItemCondition(itemElement, filial_id, conn);
                            }
                        }
                        default -> {
                            writeErrorUnknownPgoup(itemElement);
                        }
                    }
                } else {
                    addItemCondition(itemElement, filial_id, conn);
                    // writeErrorAlreadyInDB(itemElement);
                }
            }
        }
    }

    /**
     * Methode in der durch das Dokument geparsed wird, dabei wird für jedes Produkt-Tupel die zugehörigen sindÄhnlich Tupel in die Datenbank geladen.
     *
     * @param shopElement Wurzel Element des Dokumentes über dessen Kinder geparsed wird
     * @param conn        Verbindung zur Datenbank
     */
    public void addAllSimilars(Element shopElement, Connection conn) {
        NodeList children = shopElement.getChildNodes();
        for (int temp = 0; temp < children.getLength(); temp++) {
            Node nNode = children.item(temp);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element itemElement = (Element) nNode;

                String asin = itemElement.getAttribute("asin");

                if (Item.checkItem(asin, conn)) {
                    int produkt_id1 = Item.getItemID(asin, conn);
                    Element similars = (Element) itemElement.getElementsByTagName("similars").item(0);
                    NodeList items = similars.getElementsByTagName("item");
                    if (items.getLength() == 0) {
                        items = similars.getElementsByTagName("asin");
                    }
                    for (int i = 0; i < items.getLength(); i++) {
                        Element similar = (Element) items.item(i);
                        String similarASIN = similar.getAttribute("asin").equals("") ? similar.getTextContent() : similar.getAttribute("asin");
                        if (Item.checkItem(similarASIN, conn)) {
                            int produkt_id2 = Item.getItemID(similarASIN, conn);
                            if (!checkSimilarItems(produkt_id1, produkt_id2, conn)) {
                                String sql = "INSERT INTO sindÄhnlich(produkt_id1,produkt_id2)VALUES(?,?)";
                                try (
                                        PreparedStatement statement = conn.prepareStatement(sql)) {
                                    statement.setInt(1, produkt_id1);
                                    statement.setInt(2, produkt_id2);
                                    statement.executeUpdate();

                                } catch (SQLException ex) {
                                    System.err.println(ex.getMessage());
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    /**
     * Methode um aus einem XML Item-Element, wenn vorhanden, eine Kondition in die Datenbank zu laden
     *
     * @param itemElement Item-Element
     * @param filial_ID   Die Fillial_ID als Attribut der Relation Kondition
     * @param conn        Verbindung zur Datenbank
     */
    public void addItemCondition(Element itemElement, int filial_ID, Connection conn) {
        for (int x = 0; x < itemElement.getElementsByTagName("price").getLength(); x++) {
            Kondition kondition = new Kondition(itemElement, x);
            if (kondition.getErrors().isEmpty()) {
                int ID = Item.getItemID(itemElement.getAttribute("asin"), conn);
                kondition.loadKondition(kondition, ID, filial_ID, conn);
            } else {
            }
        }
    }

    /**
     * Schreibt die Errors NO ASIN UND NO TITLE für ein Item Objekt mit dem Errorwriter und passt die Error-Counter an
     *
     * @param item Das Item Objekt
     */
    public void writeErrorsOfItem(Item item) {
        try {
            ArrayList<String> errors = item.getErrors();
            if (errors.contains("NO ASIN")) {
                errorWriter.write("Produkt - " + item.getTitel() + " - besitzt keine ASIN und wurde deshalb nicht geladen!\n");
                noAsin++;
            }
            if (errors.contains("NO TITLE")) {
                errorWriter.write("Produkt mit ASIN: " + item.getProduktNummer() + " besitzt keinen Titel, und wurde deshalb nicht geladen!\n");
                noTitle++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Schreibt den Error das ein Produkt schon in der Datenbank existiert
     *
     * @param item ItemElement
     */
    public void writeErrorAlreadyInDB(Element item) {
        try {
            errorWriter.write("Produkt mit Asin : " + item.getAttribute("asin") + " ist in der DB schon enthalten!");
            itemAlreadyinDB++;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Schreibt den Error falls eine Unbekannte Art eines Produktes auftreten sollte.
     *
     * @param item Item Objekt des Produktes
     */
    public void writeErrorUnknownPgoup(Element item) {
        try {
            errorWriter.write("Produkt besitzt unbekannten Typ - " + item.getAttribute("pgroup") + " - und wurde deshalb nicht geladen\n");
            unknownPgroup++;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Läd ein Tupel Filliale in eine Datenbank
     *
     * @param plz    Attribut für Relation Filliale
     * @param straße Attribut für Relation Filliale
     * @param name   Attribut für Relation Filliale
     * @param conn   Verbindung zur Datenbank
     * @return Generierter P-Key
     */
    public int loadShop(String plz, String straße, String name, Connection conn) {
        int ShopID = -1;
        String sql = "INSERT INTO filiale(name,strasse,plz)VALUES(?,?,?)";
        int product_id = -1;
        try (
                PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.setString(2, straße);
            statement.setInt(3, Integer.parseInt(plz));
            statement.executeUpdate();
            ResultSet rs = statement.getGeneratedKeys();
            rs.next();
            ShopID = rs.getInt("filial_id");
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
        return ShopID;
    }

    /**
     * Fragt ob ein Tupel sindÄhnlich für 2 Produkt_id's in einer Datenbank existiert existiert
     *
     * @param produkt_id1 1. Produkt_id
     * @param produkt_id2 2. Produkt_id
     * @param conn        Verbindung zur Datenbank
     * @return true - wenn ja -- false wenn nicht
     */
    public boolean checkSimilarItems(int produkt_id1, int produkt_id2, Connection conn) {
        String sql = "Select * FROM sindÄhnlich WHERE produkt_id1 = ? AND produkt_id2 = ? OR produkt_id1 = ? AND produkt_id2 = ?";
        boolean rückgabe = false;
        try (
                PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, produkt_id1);
            statement.setInt(2, produkt_id2);
            statement.setInt(3, produkt_id2);
            statement.setInt(4, produkt_id1);
            ResultSet rs = statement.executeQuery();
            rückgabe = rs.next();
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }

        return rückgabe;
    }

    /**
     * Ermittelt die Error Zähler für das Dokument und gibt diese als ArrayList zurück
     *
     * @return ErrorCounters für das Dokument
     */
    public ArrayList<Integer> getErrors() {
        ArrayList<Integer> ergebnis = new ArrayList<>();
        ergebnis.add(this.unknownPgroup);
        ergebnis.add(this.noAsin);
        ergebnis.add(this.noTitle);
        ergebnis.add(this.itemAlreadyinDB);
        return ergebnis;
    }
}