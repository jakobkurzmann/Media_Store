import java.sql.*;
import java.util.ArrayList;

import org.w3c.dom.Element;

/**
 * Klasse um aus einem XML Element ein Produkt Object zu erstellen und diese in eine Datenbank zu laden
 */
public class Item {
    private String produktNummer;
    private String titel;
    private int verkaufsRang;
    private String bild;
    private String typ;
    ArrayList<String> errors;

    /**
     * Erstellt aus einem XML Item-Element ein Item Object und extrahiert dabei alle wichtigen Informationen
     * @param element - Das Item-Element
     */
    public Item(Element element, String typ){
        this.typ = typ;
        this.errors = new ArrayList<String>();
        produktNummer = element.getAttribute("asin");
        if (produktNummer.equals("")) {
            errors.add("NO ASIN");
        }
        try {
            this.verkaufsRang = Integer.parseInt(element.getAttribute("salesrank"));
        } catch (NumberFormatException e) {
        }
        Element details = (Element)element.getElementsByTagName("details").item(0);
        if (details != null) {
            this.bild = details.getAttribute("img");
        }
        try {
            titel = element.getElementsByTagName("title").item(0).getTextContent();
        } catch (NullPointerException e) {
            errors.add("NO TITLE");
        }
    }
    public void addAtributes(Element element){
        String produktNummer;
        String titel = "";
        int verkaufsRang = 0;
        String Bild = "";
        ArrayList<String> errors = new ArrayList<String>();
        produktNummer = element.getAttribute("asin");
        if (produktNummer.equals("")) {
            errors.add("NO ASIN FOUND");
        }
        try {
            verkaufsRang = Integer.parseInt(element.getAttribute("salesrank"));
        } catch (NumberFormatException e) {
        }

        Element details = (Element)element.getElementsByTagName("details").item(0);
        if (details != null) {
            Bild = details.getAttribute("img");
        }

        try {
            titel = element.getElementsByTagName("title").item(0).getTextContent();
        } catch (NullPointerException e) {
            errors.add("NO TITLE FOUND");
        }
    }
    public ArrayList<String> getErrors() {
        return errors;
    }

    public int getVerkaufsRang() {
        return verkaufsRang;
    }

    public String getBild() {
        return bild;
    }

    public String getTitel() {
        return this.titel;
    }

    public String getProduktNummer() {
        return produktNummer;
    }
    public String getTyp(){
        return this.typ;
    }

    /**
     * Fragt ob in einer Datenbank ein Tupel mit einer gegebenen ASIN existiert
     * @param asin - die asin
     * @param conn - Verbindung zur Datenbank
     * @return true -> es existiert eins -- false -> es existiert keins
     */
    public static boolean checkItem(String asin, Connection conn){
        boolean rückgabe = false;
        String sql ="SELECT produkt_id FROM produkt WHERE asin = ?";
        int product_id = -1;
        try(
            PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))

        {
            statement.setString(1,asin);
            ResultSet rs = statement.executeQuery();
            rückgabe = rs.next();
        }catch (SQLException ex)
        {
            System.err.println(ex.getMessage());
        }
        return rückgabe;
    }

    /**
     * Fragt nach der Produkt_ID eines Tupels der Tabelle Produkt für eine gegebene ASIN
     * @param asin - gegebene Asin
     * @param conn - Verbindung zur Datenbank
     * @return  die ProduktID oder -1 falls nicht existiert
     */
    public static int getItemID(String asin, Connection conn){
        String sql ="SELECT produkt_id FROM produkt WHERE asin LIKE ?";
        int product_id = -1;
        try(
            PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))

        {
            statement.setString(1,asin);
            ResultSet rs = statement.executeQuery();
            rs.next();
            product_id = rs.getInt("produkt_ID");
        }catch (SQLException ex)
        {
            System.err.println(ex.getMessage());
        }
        return product_id;
    }

    /**
     * Methode um ein Item Object in eine Datenbank zu laden (Relation Produkt)
     * @param produkt Item Object
     * @param conn Verbindung zur Datenbank
     * @return Gibt generierten P-Key des Tupels zurück
     */
    public static int loadItem(Item produkt, Connection conn){
        String sql ="INSERT INTO produkt(asin,titel,bild,verkaufsrang,typ)VALUES(?,?,?,?,?)";
        int product_id = -1;
        try(
            PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))

        {
            statement.setString(1,produkt.getProduktNummer());
            statement.setString(2,produkt.getTitel());
           // statement.setDouble(3,);
            statement.setString(3, produkt.getBild());
            statement.setInt(4,produkt.getVerkaufsRang());
            statement.setString(5, produkt.getTyp());
            statement.executeUpdate();
            ResultSet test = statement.getGeneratedKeys();
            test.next();
            product_id = test.getInt("produkt_id");
        }catch (SQLException ex)
        {
            System.err.println(ex.getMessage());
        }
            return product_id;
    }

    /**
     * Ist da um Das Rating eines Tupels der Relation Produkt zu updaten
     * @param produkt_id P-Key des Tupels
     * @param Rating neues Rating
     * @param conn Verbindung zur Datenbank
     */
    public static void updateRating(int produkt_id, double Rating, Connection conn){
        String sql ="UPDATE produkt SET rating = ? Where produkt_id = ?";
        try(
            PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))

        {
            statement.setDouble(1,Rating);
            statement.setInt(2,produkt_id);
            statement.executeUpdate();
        }catch (SQLException ex)
        {
            System.err.println(ex.getMessage());
        }
    }
}
