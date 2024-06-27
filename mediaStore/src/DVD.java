import org.w3c.dom.Element;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 *  Klasse um CD aus XML Element zu erstellen und diese in die Datenbank zu laden
 *  Erbt von Item
 */
public class DVD extends Item{
    String format;
    int laufzeit;
    int regionenCode;
    ArrayList<String> actors;
    ArrayList<String> creators;
    ArrayList<String> directors;

    /**
     * Bekommt als Argument ein XML Item-Element mit pgroup=DVD und wandelt dieses in ein DVD Objekt um
     * @param element dvd element aus XML Datei
     */
    public DVD(Element element){
        super(element, "DVD");
        this.format = element.getElementsByTagName("format").item(0).getTextContent();
        try {
            this.laufzeit = Integer.parseInt(element.getElementsByTagName("runningtime").item(0).getTextContent());
        }catch(NumberFormatException e){

        }
        try {
            this.regionenCode = Integer.parseInt(element.getElementsByTagName("regioncode").item(0).getTextContent());
        }catch(NumberFormatException e){
            this.regionenCode = 0;
        }
        this.actors = new ArrayList<>();
        for (int x = 0; x < element.getElementsByTagName("actor").getLength(); x++) {
            if(element.getElementsByTagName("actor").item(x).getTextContent().equals(""))
            {
                actors.add(((Element)element.getElementsByTagName("actor").item(x)).getAttribute("name"));
            }
            else
            {
                actors.add(element.getElementsByTagName("actor").item(x).getTextContent());
            }
        }
       this.creators = new ArrayList<>();
        for (int x = 0; x < element.getElementsByTagName("creator").getLength(); x++) {
            if(element.getElementsByTagName("creator").item(x).getTextContent().equals("")){
                creators.add(((Element)element.getElementsByTagName("creator").item(x)).getAttribute("name"));
            }else {
                creators.add(element.getElementsByTagName("creator").item(x).getTextContent());
            }
        }
        this.directors = new ArrayList<>();
        for (int x = 0; x < element.getElementsByTagName("director").getLength(); x++) {
            if(element.getElementsByTagName("director").item(x).getTextContent().equals("")){
                directors.add(((Element)element.getElementsByTagName("director").item(x)).getAttribute("name"));
            }else {
                directors.add(element.getElementsByTagName("director").item(x).getTextContent());
            }
        }
    }

    public String getFormat() {
        return format;
    }

    public int getLaufzeit() {
        return laufzeit;
    }

    public int getRegionenCode() {
        return regionenCode;
    }

    public ArrayList<String> getActors() {
        return actors;
    }

    public ArrayList<String> getCreators() {
        return creators;
    }

    public ArrayList<String> getDirectors() {
        return directors;
    }

    /**
     * Methode mit prepared Statements für die Tabelle DVD, DVD_Actor,DVD_CREATOR,DVD_DIRECTOR, fügt Tupel für ein DVD Object in  die Datenbank ein
     * @param dvd Objekt welches in die Datenbank geladen wird.
     * @param conn Verbindung mit der Datenbank
     */
    public static void loadDVD(DVD dvd, Connection conn){
        int product_id = Item.loadItem(dvd, conn);
        ArrayList<String> actors = dvd.getActors();
        ArrayList<String> creators = dvd.getCreators();
        ArrayList<String> directors = dvd.getDirectors();
        String sql ="INSERT INTO dvd(produkt_id,format,laufzeit,regionencode)VALUES(?,?,?,?)";
        String sql2 = "INSERT INTO dvd_actor(produkt_id,actor)VALUES(?,?)";
        String sql3 = "INSERT INTO dvd_creator(produkt_id,creator)VALUES(?,?)";
        String sql4 = "INSERT INTO dvd_director(produkt_id,director)VALUES(?,?)";
        try(
            PreparedStatement statement = conn.prepareStatement(sql);
            PreparedStatement statement2 = conn.prepareStatement(sql2);
        PreparedStatement statement3 = conn.prepareStatement(sql3);
        PreparedStatement statement4 = conn.prepareStatement(sql4)
        )
        {
            statement.setInt(1,product_id);
            statement.setString(2,dvd.getFormat());
            statement.setInt(3,dvd.getLaufzeit());
            statement.setInt(4,dvd.getRegionenCode());
            statement.executeUpdate();

            for(String actor:actors)
            {
                statement2.setInt(1,product_id);
                statement2.setString(2,actor);
                statement2.executeUpdate();
            }
            for(String creator:creators)
            {
                statement3.setInt(1,product_id);
                statement3.setString(2,creator);
                statement3.executeUpdate();
            }
            for(String director:directors)
            {
                statement4.setInt(1,product_id);
                statement4.setString(2,director);
                statement4.executeUpdate();
            }
        }catch (SQLException ex)
        {
            System.err.println(ex.getMessage());
        }
    }
}
