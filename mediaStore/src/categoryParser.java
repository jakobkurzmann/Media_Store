import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;

/**
 * Klasse um ein XML zu parsen welches Kategorien enthält, und diese sammt ihrer Produkte in eine Datenbank zu laden.
 * Dabei schreibt sie Errors falls ein Produkt nicht in der Datenbank auftritt
 */
public class categoryParser {
    FileWriter errorWriter;
    int productNotInDB;

    /**
     * Construktor in dem eine Verbindung zu einer Datenbank aufgebaut wird, und die Methoden zum Parsen des Dokumentes aufgerufen werden
     * @param fileName Pfad zum Dokument welches geparsed werden soll
     * @param errorWriter FileWriter der die Errors schreibt
     */
    public categoryParser(String fileName, FileWriter errorWriter){
        this.errorWriter = errorWriter;
        productNotInDB = 0;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try(Connection conn = Verbindung.connect()) {
            try {

                DocumentBuilder db = dbf.newDocumentBuilder();

                Document doc = db.parse(new File(fileName));

                doc.getDocumentElement().normalize();

                Element root = doc.getDocumentElement();
                mainCategoryParser(root, conn);
            } catch (ParserConfigurationException | SAXException | IOException e) {
                e.printStackTrace();
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    /**
     * Parsed über das WurzelElement des Dokumentes und läuft dabei die ersten Kinder (also Hauptkategorien ab), fügt diese dabei in die Datenbank ein
     * und ruft für diese die Methode subCategoryParser auf.
     * @param rootElement Wurzel Element
     * @param conn Verbindung zur Datenbank
     */
    public void mainCategoryParser(Element rootElement, Connection conn){
        NodeList children = rootElement.getChildNodes();
        for (int temp = 0; temp < children.getLength(); temp++) {
            Node nNode = children.item(temp);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element categoryElement = (Element) nNode;
                Category mainCategory = new Category(categoryElement);
                int mainCategoryID = Category.loadInDB(mainCategory, conn);
                addItemsToCategory(categoryElement, mainCategoryID, conn);
                if (categoryElement.getElementsByTagName("category").getLength() > 0) {
                    subCategoryParser(categoryElement, mainCategoryID, conn);
                }
            }
        }
    }

    /**
     * Parsed über die Unterkategorien einer Mutterkategorie, lädt dabei sich selber und vorhandene Produkte in die Datenbank und führt sich selbst rekursiv aus,
     * bis für eine Kategorie keine Unterkategorie mehr vorhanden ist.
     * @param motherCategory Element der Mutter Kategorie
     * @param motherCategoryID P-Key der Mutterkategorie
     * @param conn Verbindung zur Datenbank
     */
    public void subCategoryParser(Element motherCategory, int motherCategoryID, Connection conn){
        NodeList children = motherCategory.getChildNodes();
        for (int temp = 0; temp < children.getLength(); temp++) {
            Node nNode = children.item(temp);
            if(nNode.getNodeName().equals("category")){
                Element categoryElement = (Element) nNode;
                Category subCategory = new Category(categoryElement);
                int subCategoryID = Category.loadInDB(subCategory, motherCategoryID, conn);
                addItemsToCategory(categoryElement, subCategoryID, conn);
                if(categoryElement.getElementsByTagName("category").getLength()>0){
                    subCategoryParser(categoryElement,subCategoryID, conn);
                }
            }
        }
    }

    /**
     * Lädt alle vorhandenen Produkte für ein Kategorie Element in die Datenbank ein (Relation produkt_kategorie).
     * @param categoryElement Kategorie Element
     * @param kategorie_id Id der Kategorie
     * @param conn Verbindung zur Datenbank
     */
    public void addItemsToCategory(Element categoryElement, int kategorie_id, Connection conn){
        NodeList children = categoryElement.getChildNodes();
        for(int temp = 0; temp < children.getLength(); temp++){
            Node nNode = children.item(temp);
            if(nNode.getNodeName().equals("item")){
                String asin = children.item(temp).getFirstChild().getNodeValue();
                if(Item.checkItem(asin, conn)) {
                    int produkt_id = Item.getItemID(asin, conn);
                    String sql = "INSERT INTO produkt_kategorie(kategorie_id,produkt_id)VALUES(?,?)";
                    try(
                        PreparedStatement statement = conn.prepareStatement(sql);
                    )
                    {
                        statement.setInt(2,produkt_id);
                        statement.setInt(1,kategorie_id);
                        statement.executeUpdate();
                    }
                    catch (SQLException ex)
                    {
                        System.err.println(ex);
                    }
                }else{
                    writeErrorItemNotFound(asin);
                }
            }
        }
    }

    /**
     * Schreibt den Error falls ein Produkt einer Kategorie nicht in der Datenbank vorhanden ist
     * @param asin asin des Produktes
     */
    public void writeErrorItemNotFound(String asin){
        try{
            errorWriter.write("Could not load Product with asin : " + asin + " into Table Produkt_Kategorie! Product not in DB!\n");
            productNotInDB ++;
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Gibt den Zähler des Error wieder
     * @return ArrayList mit ErrorZähler drinne
     */
    public ArrayList<Integer> getErrors(){
        ArrayList<Integer> ergebnis = new ArrayList<>();
        ergebnis.add(this.productNotInDB);
        return ergebnis;
    }
}
