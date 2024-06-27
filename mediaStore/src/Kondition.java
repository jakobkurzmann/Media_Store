import org.w3c.dom.Element;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Klasse um eine Kondition aus XML Element zu erstellen und diese in die Datenbank zu laden
 */
public class Kondition {
    private double preis;
    private boolean verfügbarkeit;
    private String zustand;
    private String waehrung;
    ArrayList<String> errors;

    /**
     * bekommt als Argument ein XML Element und wandelt dieses in ein Konditions Objekt um
     * @param element Konditions Element aus XML Datei
     */
    public Kondition(Element element, int index){
        this.errors = new ArrayList<>();
        Element price = (Element) element.getElementsByTagName("price").item(index);
        this.verfügbarkeit = false;
        if(!price.getTextContent().equals("")) {
            try {
                if((Double.parseDouble(price.getAttribute("mult"))) * Double.parseDouble(price.getTextContent()) >=0) {
                    preis = (Double.parseDouble(price.getAttribute("mult"))) * Double.parseDouble(price.getTextContent());
                    this.verfügbarkeit = true;
                }else{
                    errors.add("Kein Valider Preis");
                }
            } catch (NumberFormatException e) {
                errors.add("Keine Valider Preis");
            }
        }else{
            errors.add("Kein Preis vorhanden");
        }
        this.waehrung = price.getAttribute("currency");
        this.zustand = price.getAttribute("state");
    }
    public ArrayList<String> getErrors(){
        return this.errors;
    }

    public double getPreis() {
        return preis;
    }

    public boolean isVerfügbarkeit() {
        return verfügbarkeit;
    }

    public String getZustand() {
        return zustand;
    }

    public String getWaehrung() {
        return waehrung;
    }

    /**
     * Methode mit prepared Statements für die Tabelle Kondition, fügt Kondition- Object in Datenbank ein
     * @param kondition  KonditionsObjekt welches in die Datenbank geladen wird.
     * @param produkt_id Die Produktid, um welches Produkt es sich bei der Kondition handelt
     * @param filial_id Die Filialid, in der diese Kondition angeboten wird
     * @param conn Verbindung zur Datenbank
     */
    public void loadKondition(Kondition kondition,int produkt_id, int filial_id, Connection conn)
    {
        String sql  = "INSERT INTO kondition(preis,zustand,waehrung,produkt_id,filial_id)VALUES(?,?,?,?,?)";
        try(
            PreparedStatement statement = conn.prepareStatement(sql))
        {
            statement.setDouble(1,kondition.getPreis());
            statement.setString(2,kondition.getZustand());
            statement.setString(3, kondition.getWaehrung());
            statement.setInt(4,produkt_id);
            statement.setInt(5,filial_id);
            statement.executeUpdate();
        }
        catch (SQLException ex)
        {
            System.out.println(ex.getMessage());
        }
    }
}
