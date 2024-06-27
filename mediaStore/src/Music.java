import org.w3c.dom.Element;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Klasse um CD aus XML Element zu erstellen und diese in die Datenbank zu laden
 * Erbt von Item
 */
public class Music extends Item {
    ArrayList<String> labels;
    Date erscheinungsDatum;
    ArrayList<String> artists;
    ArrayList<String> titelListe;

    /**
     * Bekommt als Argument ein XML Element und wandelt dieses in ein Music Objekt
     * @param element music Element aus XML Datei
     */
    public Music(Element element) {
        super(element, "CD");
        this.artists = new ArrayList<>();
        for (int x = 0; x < element.getElementsByTagName("artist").getLength(); x++) {
            if(element.getElementsByTagName("artist").item(x).getTextContent().equals("")){
                artists.add(((Element)element.getElementsByTagName("artist").item(x)).getAttribute("name"));
            }else {
                artists.add(element.getElementsByTagName("artist").item(x).getTextContent());
            }
        }
        this.labels = new ArrayList<>();
        for (int x = 0; x < element.getElementsByTagName("label").getLength(); x++) {
            if(element.getElementsByTagName("label").item(x).getTextContent().equals("")){
                labels.add(((Element)element.getElementsByTagName("label").item(x)).getAttribute("name"));
            }else {
                labels.add(element.getElementsByTagName("label").item(x).getTextContent());
            }
        }
        try{
            this.erscheinungsDatum = Date.valueOf(element.getElementsByTagName("releasedate").item(0).getTextContent());
        }catch(IllegalArgumentException e){
        }
        this.titelListe = new ArrayList<>();
        Element tracks = (Element) element.getElementsByTagName("tracks").item(0);
        for (int x = 1; x < tracks.getElementsByTagName("title").getLength(); x++) {
            titelListe.add(tracks.getElementsByTagName("title").item(x).getTextContent());
        }
    }

    public ArrayList<String> getLabels() {
        return labels;
    }

    public Date getErscheinungsDatum() {
        return this.erscheinungsDatum;
    }

    public ArrayList<String> getArtists() {
        return artists;
    }

    public ArrayList<String> getTitelListe() {
        return titelListe;
    }

    /**
     * Methode mit prepared Statements für die Tabelle CD, CD-Artist,CD_Label,CD_Titel,fügt Upel für ein Music Object in Datenbank ein
     * @param cd - MusicObjekt welches in die Datenbank geladen wird.
     * @param conn Verbindung zur Datenbank
     */
    public static void loadMusic(Music cd, Connection conn) {
        int product_ID = Item.loadItem(cd, conn);
        ArrayList<String> artists = cd.getArtists();
        ArrayList<String>  labels = cd.getLabels();
        ArrayList<String> titelListe = cd.getTitelListe();
        String sql = "INSERT INTO cd(produkt_id,erscheinungsdatum)VALUES(?,?)";
        String sql2 ="INSERT INTO cd_artist(produkt_id,artist)VALUES(?,?)";
        String sql3 ="INSERT INTO cd_label(produkt_id,label)VALUES(?,?)";
        String sql4 = "INSERT INTO cd_titel(produkt_id,titel)VALUES(?,?)";
        try (
                PreparedStatement statement = conn.prepareStatement(sql);
                PreparedStatement statement2 = conn.prepareStatement(sql2);
        PreparedStatement statement3 = conn.prepareStatement(sql3);
        PreparedStatement statement4 = conn.prepareStatement(sql4))
        {
            statement.setInt(1,product_ID);
            statement.setDate(2, cd.getErscheinungsDatum());
            statement.executeUpdate();

            for(String artist:artists)
            {
                statement2.setInt(1,product_ID);
                statement2.setString(2,artist);
                statement2.executeUpdate();
            }
            for(String label:labels)
            {
                statement3.setInt(1,product_ID);
                statement3.setString(2,label);
                statement3.executeUpdate();
            }
            for(String titel:titelListe)
            {
                statement4.setInt(1,product_ID);
                statement4.setString(2,titel);
                statement4.executeUpdate();
            }
        } catch (
                SQLException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
