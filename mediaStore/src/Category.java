import org.w3c.dom.Element;

import java.sql.*;

/**
 * Klasse um Kategorie aus XML Element zu erstellen und diese in die Datenbank zu laden
 */
public class Category  {
    public String name;

    /**
     * Bekommt als Argument ein XML Element und wandelt dieses in ein Category Objekt um
     * @param categoryElement category Element aus XML Datei
     */
    public Category(Element categoryElement){
        this.name = categoryElement.getFirstChild().getNodeValue().replace("\n","");
    }
    public String getName()
    {
        return name;
    }

    /**
     * Methode mit prepared Statements für die Tabelle Kategorie, fügt ein Kategorie Object in  die Datenbank ein
     * Diese Methode ist für Kategorien, die eine Überkategorie besitzen
     * @param kategorie ist ein KategorieObject, für welches ein Tupel in die Datenbank geladen wird
     * @param ueberKategorieID falls eine Überkategorie existiert wird diese hier mit der Kategorie ID übergeben
     * @param conn Verbindung für die Statements
     * @return gibt die generierte ID Kategorie zurück
     */
    public static int loadInDB(Category kategorie, int ueberKategorieID, Connection conn)
    {
        String sql = "INSERT INTO kategorie(bezeichnung,ueberkategorie_id)VALUES (?,?)";
        int kategorie_id=-1;
        try(
            PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        )
        {
            statement.setString(1, kategorie.getName());
            statement.setInt(2,ueberKategorieID);
            statement.executeUpdate();
            ResultSet test = statement.getGeneratedKeys();
            test.next();
            kategorie_id = test.getInt("kategorie_id");
        }
        catch (SQLException ex)
        {
            System.err.println(ex);
        }
        return kategorie_id; //return CategoryID
    }
    /**
     * Methode mit prepared Statements für die Tabelle Kategorie, baut eine Verbindung auf und fügt in Datenbank ein (für Hauptkategorien)
     * @param kategorie ist ein KategorieObject
     * @return gibt die generierte ID Kategorie zurück
     */
    public static int loadInDB(Category kategorie, Connection conn)
    {
        String sql = "INSERT INTO kategorie(bezeichnung)VALUES (?)";
        int kategorie_id=-1;
        try(
            PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        )
        {
            statement.setString(1, kategorie.getName());
            statement.executeUpdate();
            ResultSet test = statement.getGeneratedKeys();
            test.next();
            kategorie_id = test.getInt("kategorie_id");
        }
        catch (SQLException ex)
        {
            System.err.println(ex);
        }
        return kategorie_id; //return CategoryID
    }
}
