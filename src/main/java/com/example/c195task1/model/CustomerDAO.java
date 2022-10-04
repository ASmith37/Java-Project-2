package com.example.c195task1.model;

import com.example.c195task1.helper.DBConnection;
import com.example.c195task1.helper.UserData;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class CustomerDAO {
    // get all customers
    public static ObservableList<Customer> getAllCustomers() throws SQLException {
        ObservableList<Customer> allUsers = FXCollections.observableArrayList();
        DBConnection.openConnection();
        String sql = "SELECT Customer_ID, Customer_Name, Address, Postal_Code, Phone, Division_ID, Division, Country_ID, Country\n" +
                "FROM customers\n" +
                "LEFT JOIN first_level_divisions\n" +
                "USING (Division_ID)\n" +
                "LEFT JOIN countries\n" +
                "USING (Country_ID);";
        Statement s = DBConnection.connection.createStatement();
        ResultSet r;
        r = s.executeQuery(sql);
        while (r.next()) {
            // add to list
            allUsers.add(new Customer(
                    r.getInt(1),
                    r.getString(2),
                    r.getString(3),
                    r.getString(4),
                    r.getString(5),
                    r.getInt(6),
                    r.getString(7),
                    r.getInt(8),
                    r.getString(9)
                    ));
        }
        DBConnection.closeConnection();
        return allUsers;
    }
    public static int generateCustomerId() throws SQLException {
        DBConnection.openConnection();
        String sql1 = "SELECT Count(*) FROM customers;";
        Statement st1 = DBConnection.connection.createStatement();
        ResultSet rs1 = st1.executeQuery(sql1);
        rs1.next();
        if (rs1.getInt(1) == 0) {
            DBConnection.closeConnection();
            return 1;
        }
        String sql2 = "SELECT MAX(Customer_ID) FROM customers;";
        Statement st2 = DBConnection.connection.createStatement();
        ResultSet rs2 = st2.executeQuery(sql2);
        rs2.next();
        int result = rs2.getInt(1) + 1;
        DBConnection.closeConnection();
        return result;
    }
    public static void addUpdateCustomer(Customer customer, boolean isAnUpdate) throws SQLException {
        String sql;
        if (isAnUpdate) {
            sql = String.format("UPDATE customers\n" +
                    "SET Customer_Name = '%s', Address = '%s', Postal_Code = '%s', Phone = '%s', Division_ID = %d, Last_Updated_By = '%s', Last_Update = NOW()\n" +
                    "WHERE Customer_id = %d", customer.getName(), customer.getAddress(), customer.getPostCode(), customer.getPhone(), customer.getDivisionId(), UserData.username, customer.getId());
        }
        else {
            sql = String.format("INSERT INTO customers (Customer_ID, Customer_Name, Address, Postal_Code, Phone, Division_ID, Created_By, Last_Updated_By, Create_Date, Last_Update)\n" +
                    "VALUES (%d, '%s', '%s', '%s', '%s', %d, '%s', '%s', NOW(), NOW())",
                    customer.getId(), customer.getName(), customer.getAddress(), customer.getPostCode(), customer.getPhone(), customer.getDivisionId(), UserData.username, UserData.username);
        }
        System.out.println(sql);
        DBConnection.openConnection();
        Statement st1 = DBConnection.connection.createStatement();
        st1.executeUpdate(sql);
        DBConnection.closeConnection();
    }
}
