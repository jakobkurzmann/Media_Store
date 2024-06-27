import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.sql.Date;

/**
 * Klasse Review, beistzt alle Attribute eines Reviews die aus einer CSV gezogen werden
 * Besitzt Methode um ein Review Object in Datenbank zu laden
 */
public class Review {
    String asin;
    int rating;
    int helpful;
    Date reviewDate;
    String user;
    String summary;
    String comment;

    ArrayList<String> errors;

    /**
     * basic Constructor belegt alle Attribute mit den Parametern
     * @param asin
     * @param rating
     * @param helpful
     * @param reviewDate
     * @param user
     * @param summary
     * @param comment
     * @param errors
     */
    public Review(String asin, int rating, int helpful, String reviewDate, String user, String summary, String comment, ArrayList<String> errors) {
        this.asin = asin;
        this.rating = rating;
        this.helpful = helpful;
        try {
            this.reviewDate = Date.valueOf(reviewDate);
        } catch(IllegalArgumentException e){
        }
        this.user = user;
        this.summary = summary;
        this.comment = comment;
        this.errors = errors;
    }
    public ArrayList<String> getErrors() {
        return errors;
    }

    public Date getReviewDate() {
        return this.reviewDate;
    }

    public int getHelpful() {
        return helpful;
    }

    public String getAsin() {
        return asin;
    }

    public int getRating() {
        return rating;
    }
    public String getUser(){
        return this.user;
    }

    public String getComment() {
        return comment;
    }

    public String getSummary() {
        return summary;
    }

    /**
     * Formatiert alle Attribute zu einem String
     * @return The String
     */
    public String writeReview(){
        String ergebnis = getAsin() + " - ";
        if(errors.contains("Invalid Rating")){
            ergebnis = ergebnis + "Invalid Rating - ";
        }else{
            ergebnis = ergebnis + getRating() + " - ";
        }
        if(errors.contains("Invalid Helpful")){
            ergebnis = ergebnis + "Invalid Helpful - ";
        }else{
            ergebnis = ergebnis + getHelpful() + " - ";
        }
        ergebnis = ergebnis + getReviewDate() + " - ";
        ergebnis = ergebnis + getUser() + " - ";
        ergebnis = ergebnis + getSummary();
        return ergebnis;
    }

    /**
     * Methode um ein Review Objekt in eine Datenbank zu laden (in die Relation review)
     * @param review das Review Objekt
     * @param conn Verbindung zur Datenbank
     */
    public static void insertReview(Review review, Connection conn)
    {
        String sql = "INSERT INTO review(rating,helpful,reviewDate,benutzer,summary,comment,produkt_id)VALUES(?,?,?,?,?,?,?)";
        try(
            PreparedStatement statement = conn.prepareStatement(sql)){
            statement.setInt(1,review.getRating());
            statement.setInt(2,review.getHelpful());
            statement.setDate(3,review.getReviewDate());
            statement.setString(4,review.getUser());
            statement.setString(5,review.getSummary());
            statement.setString(6, review.getComment());
            statement.setInt(7,Item.getItemID(review.getAsin(),conn));
            statement.executeUpdate();
        } catch (SQLException ex)
        {
            System.err.println(ex);
        }
    }

}
