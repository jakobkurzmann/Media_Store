import org.w3c.dom.Element;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
/**
 * Klasse um Buch aus XML Element zu erstellen und diese in die Datenbank zu laden.
 * Erbt von Item da jedes Buch auch ein Produkt ist
 */
public class Book extends Item{
    private int seitenzahl;
    private Date erscheinungsDatum;
    private String isbn;
    private ArrayList<String> autoren;
    private ArrayList<String> verlaege;
    /**
     * Konstruktor -> Bekommt ein Item Element übergeben und erstellt ein Book Element
     * @param element Item Element aus XML Datei mit pgroup Book
     */
    public Book(Element element) {
        super(element, "Buch");
        this.autoren = new ArrayList<>();
        for (int x = 0; x < element.getElementsByTagName("author").getLength(); x++) {
            if(element.getElementsByTagName("author").item(x).getTextContent().equals("")){
                autoren.add(((Element)element.getElementsByTagName("author").item(x)).getAttribute("name"));
            }else {
                autoren.add(element.getElementsByTagName("author").item(x).getTextContent());
            }
        }
        try {
            if(element.getElementsByTagName("pages").item(0) != null) {
                this.seitenzahl = Integer.parseInt(element.getElementsByTagName("pages").item(0).getTextContent());
            }
        }catch(NumberFormatException e){
            this.seitenzahl = 0;
        }
        try {
            this.erscheinungsDatum = Date.valueOf(((Element) element.getElementsByTagName("publication").item(0)).getAttribute("date"));
        }catch(IllegalArgumentException e){
        }
        this.isbn = ((Element)element.getElementsByTagName("isbn").item(0)).getAttribute("val");
        this.verlaege = new ArrayList<>();
        for (int x = 0; x < element.getElementsByTagName("publisher").getLength(); x++) {
            if(element.getElementsByTagName("publisher").item(x).getTextContent().equals("")){
                verlaege.add(((Element)element.getElementsByTagName("publisher").item(x)).getAttribute("name"));
            }else {
                verlaege.add(element.getElementsByTagName("publisher").item(x).getTextContent());
            }
        }
    }

    public int getSeitenzahl() {
        return seitenzahl;
    }

    public Date getErscheinungsDatum() {
        return this.erscheinungsDatum;
    }

    public String getIsbn() {
        return isbn;
    }

    public ArrayList<String> getAutoren() {
        return autoren;
    }

    public ArrayList<String> getVerlaege() {
        return verlaege;
    }
    /**
     * Methode mit prepared Statements für die Tabelle Buch, Buch_Autor,Buch_Verlag, fügt Tupel für ein Book Objecct in die Datenbank der Verbindung
     * Conn ein
     * @param book BookObjekt welches in die Datenbank geladen wird.
     * @param conn Verbindung mit der Datenbank um Statement auszuführen
     */
    public static void loadBook(Book book, Connection conn){
        int produkt_id = Item.loadItem(book, conn);
        ArrayList<String> autoren = book.getAutoren();
        ArrayList<String> verlaege = book.getVerlaege();
        String sql ="INSERT INTO buch(produkt_id,seitenzahl,erscheinungsdatum,isbn)VALUES(?,?,?,?)";
        String sql1="INSERT INTO buch_autor(produkt_id,autor)VALUES(?,?)";
        String sql2="INSERT INTO buch_verlag(produkt_id,verlag)VALUES(?,?)";
        try(
            PreparedStatement statement = conn.prepareStatement(sql);
            PreparedStatement statement2 = conn.prepareStatement(sql1);
            PreparedStatement statement3 = conn.prepareStatement(sql2))
        {
            statement.setInt(1,produkt_id);
            statement.setInt(2,book.getSeitenzahl());
            statement.setDate(3,book.getErscheinungsDatum());
            statement.setString(4,book.getIsbn());
            statement.executeUpdate();

            for(String autor: autoren) {
                statement2.setInt(1, produkt_id);
                statement2.setString(2,autor);
                statement2.executeUpdate();
            }

            for(String verlag:verlaege){
                statement3.setInt(1,produkt_id);
                statement3.setString(2,verlag);
                statement3.executeUpdate();
            }
        }catch (SQLException ex)
        {
            System.err.println(ex.getMessage());
        }
    }
    }

