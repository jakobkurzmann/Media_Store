import java.io.*;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
/**
 * Klasse die für das Parsen einer CSV mit enthaltenen Review zuständig ist
 */
public class ReviewParser {
    FileWriter errorWriter;
    int productNotInDB;
    int ratingOutOfRange;

    /**
     * Konstruktor der eine Datenbankverbindung erstellt, und mittels eines BufferedReaders über die Zeilen der Datei iterriert und für diese
     * ein Review Objekt erstellt. Wenn das Review objekt valide ist wird eine Methode zum Laden in die Datenbank aufgerufen, falls nicht wird ein
     * Error über den Errorwriter geschrieben.
     * Zum Schluss werden die Ratings der Produkte aktualisiert.
     * @param filename Pfad zur CSV Datei
     * @param fileWriter Filewriter zum schreiben der Errors
     */
    public ReviewParser(String filename, FileWriter fileWriter){
        errorWriter = fileWriter;
        try(Connection conn = Verbindung.connect()) {
            try {
                FileReader fr = new FileReader(filename);
                BufferedReader reader = new BufferedReader(fr);
                reader.readLine();
                String line = reader.readLine();
                while (line != null) {
                    String[] eigenschaften = line.split("\",\"");
                    Review review = createReview(eigenschaften, conn);
                    if (review.getErrors().size() > 0) {
                        writeReviewErrors(review);
                    } else {
                        Review.insertReview(review, conn);
                    }
                    line = reader.readLine();
                }
                updateRating(conn);
            } catch (IOException e) {

            }
        }catch(SQLException ex){
            ex.printStackTrace();
        }
    }

    /**
     * Erstellt aus einem Array String ein Review und gibt dieses zurück, fragt ob das zugehörige Produkt in der DB falls nicht wird ein Error
     * ausgegeben
     * @param eigenschaften Der ArrayString aus dem die Attribute des Reviews gezogen werden
     * @param conn Verbindung zur Datenbank
     * @return Review Objekt
     */
    public Review createReview(String[] eigenschaften, Connection conn){
        ArrayList<String> errors = new ArrayList<>();
        String asin = eigenschaften[0].substring(1);
        int rating = -1;
        int helpful = -1;
        if(!Item.checkItem(eigenschaften[0].substring(1), conn)){
            errors.add("Product not in DB");
        }
        try{
            if(Integer.parseInt(eigenschaften[1]) < 0 || Integer.parseInt(eigenschaften[1]) > 5){
                errors.add("Invalid Rating");
                rating = Integer.parseInt(eigenschaften[1]);
            } else{
                rating = Integer.parseInt(eigenschaften[1]);
            }
        }catch(NumberFormatException e){
            errors.add("Invalid Rating");
        }
        helpful = Integer.parseInt(eigenschaften[2]);
        String reviewDate = eigenschaften[3];
        String userName = eigenschaften[4];
        String summary = eigenschaften[5];
        String content = eigenschaften[6].substring(0,eigenschaften[6].length() -1);
        return new Review(asin, rating, helpful, reviewDate, userName, summary, content, errors);
    }

    /**
     * Schreibt mittels des Errorwriters, Errors für ein Review und akutalisiert die Zähler
     * @param review Das Review für das die Errors geschrieben werden sollen
     */
    public void writeReviewErrors(Review review){
        try {
            errorWriter.write("Could not load Review - Error: ");
            if(review.getErrors().contains("Product not in DB")){
                productNotInDB++;
            }
            if(review.getErrors().contains("Invalid Rating")){
                ratingOutOfRange++;
            }
            for(String s: review.getErrors()){
                errorWriter.write(s);
            }
            errorWriter.write(" - " + review.writeReview() + "\n");
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Methode mit der für jedes Produkt zu dem Ein Review existiert, das Durchschnitlliche rating ermittelt und aktualisiert wird
     * @param conn Verbindung zur Datenbank
     */
    public void updateRating(Connection conn){
        String sql1 = "SELECT produkt_id,AVG(rating) AS rating FROM review GROUP BY produkt_id";
        try(
            Statement statement = conn.createStatement();
            ResultSet rs=statement.executeQuery(sql1);
        )
        {
            while (rs.next())
            {
              int produkt_id = rs.getInt("produkt_id");
              double rating = rs.getDouble("rating");
              Item.updateRating(produkt_id, rating, conn);
            }
        }
        catch (SQLException ex)
        {
            System.err.println(ex.getMessage());
        }
    }

    /**
     * Gibt die ErrorZähler zurück
     * @return Errorzähler
     */
    public ArrayList<Integer> getErrors(){
        ArrayList<Integer> ergebnis = new ArrayList<>();
        ergebnis.add(this.productNotInDB);
        ergebnis.add(this.ratingOutOfRange);
        return ergebnis;
    }
}
