/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author Admin
 */
public class DbConnection {
    private static final String USER = "root";
    private static final String PASS = ""; 
    private static final String URL = "jdbc:mysql://localhost:3309/caro_game";
    public static Connection getConnection() {
        Connection conn = null;
        try {
           
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASS);
            System.out.println("Kết nối Database thành công!");
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Lỗi kết nối Database: " + e.getMessage());
            e.printStackTrace();
        }
        return conn;
    }
    
   
    public static void main(String[] args) {
        getConnection();
    }
}
