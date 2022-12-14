package com.example.c195task1.model;

import com.example.c195task1.helper.DBConnection;
import com.example.c195task1.helper.UserData;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Hold the DAO methods for the Appointment class
 */
public class AppointmentDAO {
    /** Converts a LocalDateTime in the user's timezone to EST
     * @param ldt the user's LocalDateTime
     * @return a LocalDateTime of the time in EST
     */
    private static LocalDateTime localDateTimeToEst(LocalDateTime ldt) {
        return ZonedDateTime.ofInstant(ZonedDateTime.of(ldt, ZoneId.systemDefault()).toInstant(), ZoneId.of("Etc/GMT+5")).toLocalDateTime();
    }

    /**
     * Converts an EST time to the user's local time
     * @param est the time in EST
     * @return a LocalDateTime in the user's timezone
     */
    private static LocalDateTime estLocalDateTimeToLocal(LocalDateTime est) {
        return ZonedDateTime.ofInstant(ZonedDateTime.of(est, ZoneId.of("Etc/GMT+5")).toInstant(), ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * Gets a list of all the appointments in the database
     * @return a list of Appointments
     * @throws SQLException
     */
    public static ObservableList<Appointment> getAllAppointments() throws SQLException {
        ObservableList<Appointment> allAppointments = FXCollections.observableArrayList();
        DBConnection.openConnection();
        String sql = "SELECT Appointment_ID, `Title`, `Description`, Location, `Type`, `Start`, `End`, Create_Date, Created_By, Last_Update, Last_Updated_By, Customer_ID, User_ID, Contact_ID, Contact_Name\n" +
                "FROM appointments\n" +
                "LEFT JOIN contacts\n" +
                "USING (Contact_ID);";
        Statement s = DBConnection.connection.createStatement();
        ResultSet r;
        r = s.executeQuery(sql);
        while (r.next()) {
            // add to list
            allAppointments.add(new Appointment(
                    r.getInt(1),
                    r.getString(2),
                    r.getString(3),
                    r.getString(4),
                    r.getString(5),
                    r.getTimestamp(6).toLocalDateTime(),
                    r.getTimestamp(7).toLocalDateTime(),
                    r.getTimestamp(8).toLocalDateTime(),
                    r.getString(9),
                    r.getTimestamp(10).toLocalDateTime(),
                    r.getString(11),
                    r.getInt(12),
                    r.getInt(13),
                    r.getInt(14),
                    r.getString(15)
            ));
        }
        DBConnection.closeConnection();

        return allAppointments;
    }

    /**
     * Generates a new, unique, appointment ID
     * @return integer of unused appointment ID
     * @throws SQLException
     */
    public static int generateAppointmentId() throws SQLException {
        DBConnection.openConnection();
        String sql1 = "SELECT Count(*) FROM appointments;";
        Statement st1 = DBConnection.connection.createStatement();
        ResultSet rs1 = st1.executeQuery(sql1);
        rs1.next();
        if (rs1.getInt(1) == 0) {
            DBConnection.closeConnection();
            return 1;
        }
        String sql2 = "SELECT MAX(Appointment_ID) FROM appointments;";
        Statement st2 = DBConnection.connection.createStatement();
        ResultSet rs2 = st2.executeQuery(sql2);
        rs2.next();
        int result = rs2.getInt(1) + 1;
        DBConnection.closeConnection();
        return result;
    }

    /**
     * Checks to see if there are any overlapping appointments for the same customer
     * @param appointment The appointment in question
     * @return boolean value indicating if another appointment in the database overlaps this one
     * @throws SQLException
     */
    public static boolean isThereOverlappingAppointment(Appointment appointment) throws SQLException {
        DBConnection.openConnection();
        PreparedStatement ps = DBConnection.connection.prepareStatement(
                "SELECT COUNT(*)\n" +
                        "FROM appointments\n" +
                        "WHERE Customer_ID = ?\n" +
                        "AND Appointment_ID != ?\n" +
                        "AND Start <= ?\n" +
                        "AND End >= ?"
        );
        ps.setInt(1, appointment.getCustomerId());
        ps.setInt(2, appointment.getAppointmentId());
        ps.setTimestamp(3, Timestamp.valueOf(appointment.getEnd()));
        ps.setTimestamp(4, Timestamp.valueOf(appointment.getStart()));
        ResultSet r = ps.executeQuery();
        r.next();
        int count = r.getInt(1);
        DBConnection.closeConnection();
        return count > 0;
    }

    /**
     * Adds or updates an appointment in the database
     * @param appointment an appointment object filled with data
     * @param isAnUpdate true if the appointment exists and is being modified, false if the appointment is new
     * @throws SQLException
     */
    public static void addUpdateAppointment(Appointment appointment, boolean isAnUpdate) throws SQLException {
        PreparedStatement ps;
        DBConnection.openConnection();
        if (isAnUpdate) {
            ps = DBConnection.connection.prepareStatement(
                    "UPDATE appointments\n" +
                    "SET `Title` = ?, `Description` = ?, Location = ?, `Type` = ?, `Start` = ?, `End` = ?, Last_Update = NOW(), Last_Updated_By = ?, Customer_ID = ?, User_ID = ?, Contact_ID = ?\n" +
                    "WHERE Appointment_ID = ?");
            ps.setString(1, appointment.getTitle());
            ps.setString(2, appointment.getDescription());
            ps.setString(3, appointment.getLocation());
            ps.setString(4, appointment.getType());
            ps.setTimestamp(5, Timestamp.valueOf(appointment.getStart()));
            ps.setTimestamp(6, Timestamp.valueOf(appointment.getEnd()));
            ps.setString(7, UserData.username);
            ps.setInt(8, appointment.getCustomerId());
            ps.setInt(9, appointment.getUserId());
            ps.setInt(10, appointment.getContactId());
            ps.setInt(11, appointment.getAppointmentId());
        }
        else {
            ps = DBConnection.connection.prepareStatement(
                    "INSERT INTO appointments (Appointment_ID, `Title`, `Description`, Location, `Type`, `Start`, `End`, Create_Date, Created_By, Last_Update, Last_Updated_By, Customer_ID, User_ID, Contact_ID)\n" +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), ?, NOW(), ?, ?, ?, ?)");
            ps.setInt(1, appointment.getAppointmentId());
            ps.setString(2, appointment.getTitle());
            ps.setString(3, appointment.getDescription());
            ps.setString(4, appointment.getLocation());
            ps.setString(5, appointment.getType());
            ps.setTimestamp(6, Timestamp.valueOf(appointment.getStart()));
            ps.setTimestamp(7, Timestamp.valueOf(appointment.getEnd()));
            ps.setString(8, UserData.username);
            ps.setString(9, UserData.username);
            ps.setInt(10, appointment.getCustomerId());
            ps.setInt(11, appointment.getUserId());
            ps.setInt(12, appointment.getContactId());
        }
        ps.executeUpdate();
        DBConnection.closeConnection();
    }

    /**
     * Deletes the specified appointment from the database
     * @param appointment the appointment to delete
     * @throws SQLException
     */
    public static void deleteAppointment(Appointment appointment) throws SQLException {
        String sql = "DELETE FROM appointments\n" +
                "WHERE Appointment_ID = " + String.valueOf(appointment.getAppointmentId());
        DBConnection.openConnection();
        Statement statement = DBConnection.connection.createStatement();
        statement.executeUpdate(sql);
        DBConnection.closeConnection();
    }

    /**
     * Check the database to see if there is an appointment starting in the next 15 minutes
     * @return an appointment starting soon, or null if no appointments start soon
     * @throws SQLException
     */
    public static Appointment checkForAppointmentSoon() throws SQLException {
        DBConnection.openConnection();
        PreparedStatement ps = DBConnection.connection.prepareStatement(
                "SELECT Appointment_ID, `Title`, `Description`, Location, `Type`, `Start`, `End`, Create_Date, Created_By, Last_Update, Last_Updated_By, Customer_ID, User_ID, Contact_ID, Contact_Name\n" +
                        "FROM appointments\n" +
                        "LEFT JOIN contacts\n" +
                        "USING (Contact_ID)\n" +
                        "WHERE Start >= ?\n" +
                        "AND Start <= date_add(?, interval 15 minute);"
        );
        ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
        ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
        ResultSet r = ps.executeQuery();
        if (r.next()) {
            Appointment appointment = new Appointment(
                    r.getInt(1),
                    r.getString(2),
                    r.getString(3),
                    r.getString(4),
                    r.getString(5),
                    r.getTimestamp(6).toLocalDateTime(),
                    r.getTimestamp(7).toLocalDateTime(),
                    r.getTimestamp(8).toLocalDateTime(),
                    r.getString(9),
                    r.getTimestamp(10).toLocalDateTime(),
                    r.getString(11),
                    r.getInt(12),
                    r.getInt(13),
                    r.getInt(14),
                    r.getString(15));
            DBConnection.closeConnection();
            return appointment;
        }
        else {
            DBConnection.closeConnection();
            return null;
        }
    }
}
