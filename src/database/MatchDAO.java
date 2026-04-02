/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
public class MatchDAO {
     public void saveMatch(String winner, String loser, int moves) {
        saveMatchWithResult(winner, loser, moves, "WIN");
    }
    
    // Lưu trận hòa
    public void saveDrawMatch(String player1, String player2, int moves) {
        saveMatchWithResult(player1, player2, moves, "DRAW");
    }
    
    // Lưu trận timeout
    public void saveTimeoutMatch(String winner, String loser, int moves) {
        saveMatchWithResult(winner, loser, moves, "TIMEOUT");
    }
    
    // Phương thức chung để lưu match
    private void saveMatchWithResult(String player1, String player2, int moves, String result) {
        String sql = "INSERT INTO matches (winner_username, loser_username, moves_count, result) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, player1);
            stmt.setString(2, player2);
            stmt.setInt(3, moves);
            stmt.setString(4, result);
            
            stmt.executeUpdate();
            System.out.println("Đã lưu trận đấu (" + result + ") vào Database!");
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
