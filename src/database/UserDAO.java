/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package database;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;import java.util.List;
import java.util.ArrayList;import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Stack;
/**
 *
 * @author Admin
 */
public class UserDAO {
    private String hashPassword(String plainPassword, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            
            String input = plainPassword + salt; 
            
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (int i = 0; i < encodedhash.length; i++) {
                String hex = Integer.toHexString(0xff & encodedhash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String normalizeAvatar(String avatar) {
        if (avatar == null || avatar.trim().isEmpty()) {
            return "1.jpg";
        }
        // Đảm bảo có đuôi .jpg
        if (!avatar.endsWith(".jpg")) {
            return avatar + ".jpg";
        }
        return avatar;
    }

    // 3. Sửa hàm Đăng ký: Tạo salt và lưu vào DB
    public boolean register(String username, String password, String email) {
        // Tạo muối ngẫu nhiên
        String salt = UUID.randomUUID().toString();
        
        // Hash password cùng với muối
        String hashedPassword = hashPassword(password, salt); 
        
        // Câu lệnh SQL thêm cột salt và avatar - sử dụng score thay vì elo
        String sql = "INSERT INTO users (username, password, email, salt, avatar, score, wins, losses) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.setString(3, email);
            stmt.setString(4, salt); // Lưu muối vào cột thứ 4
            stmt.setString(5, "1.jpg"); // Avatar mặc định
            stmt.setInt(6, 1000); // Score mặc định (thay vì Elo)
            stmt.setInt(7, 0); // Wins
            stmt.setInt(8, 0); // Losses
            
            int rowsInserted = stmt.executeUpdate();
            return rowsInserted > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 4. Sửa hàm Đăng nhập: Lấy salt ra để check
    public UserInfo checkLogin(String usernameOrEmail, String password) {
        // Lấy cả password, salt và thông tin user - sử dụng score thay vì elo
        String sql = "SELECT username, email, password, salt, score, wins, losses FROM users WHERE username = ? OR email = ?";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, usernameOrEmail);
            stmt.setString(2, usernameOrEmail);
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String dbHash = rs.getString("password");
                String dbSalt = rs.getString("salt"); 
                
                // Nếu tài khoản cũ (chưa có salt) đăng nhập -> Sẽ lỗi hoặc cần xử lý riêng
                if (dbSalt == null) {
                    System.out.println("Tài khoản cũ chưa có Salt. Vui lòng cập nhật DB.");
                    return null;
                }

                // Hash mật khẩu nhập vào VỚI CÁI SALT TRONG DB
                String inputHash = hashPassword(password, dbSalt);
                
                if (dbHash.equals(inputHash)) {
                    // Đăng nhập thành công -> Tạo object UserInfo trả về
                    UserInfo user = new UserInfo();
                    user.username = rs.getString("username");
                    user.email = rs.getString("email");
                    user.avatar = "1.jpg"; // Mặc định vì chưa có cột avatar trong DB
                    user.elo = rs.getInt("score"); // Sử dụng score thay vì elo
                    user.wins = rs.getInt("wins");
                    user.losses = rs.getInt("losses");
                    return user;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; 
    }

    
    public void updateDrawScore(String username) {
    String sql = "UPDATE users SET draws = draws + 1, score = score + 2 WHERE username = ?";
    
    try (Connection conn = DbConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        
        stmt.setString(1, username);
        stmt.executeUpdate();
        
    } catch (SQLException e) {
        e.printStackTrace();
    }
}
    public void updateScore(String username, int pointsToAdd, String resultType) {
    String sql = "";
    
    if ("WIN".equals(resultType)) {
        sql = "UPDATE users SET score = score + ?, wins = wins + 1 WHERE username = ?";
    } else if ("LOSE".equals(resultType)) {
        sql = "UPDATE users SET score = GREATEST(score + ?, 0), losses = losses + 1 WHERE username = ?";
    } else if ("DRAW".equals(resultType)) {
        sql = "UPDATE users SET score = score + ?, draws = draws + 1 WHERE username = ?";
    } else {
        // Mặc định là thắng
        sql = "UPDATE users SET score = score + ?, wins = wins + 1 WHERE username = ?";
    }
    
    try (Connection conn = DbConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        
        stmt.setInt(1, pointsToAdd);
        stmt.setString(2, username);
        stmt.executeUpdate();
        
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

    public boolean updateAvatar(String username, String avatar) {
        // Tạm thời return true vì chưa có cột avatar trong database
        // Sẽ lưu vào cache thay thế
        System.out.println("updateAvatar được gọi cho " + username + " với avatar " + avatar);
        return true;
    }

    public String getAvatar(String username) {
        
        return "1.jpg";
    }

    
    public static class UserInfo {
        public String username;
        public String email;
        public String avatar;
        public int elo;
        public int wins;
        public int losses;
        public int draws = 0;

        public double getWinRate() {
            int total = wins + losses + draws;
            if (total == 0) return 0.0;
            return (double) wins / total * 100;
        }
    }

    

    public static class RankingInfo {
        public String username;
        public String avatar;
        public int elo;
        public int wins;
        public int losses;
        public int rank;

        public double getWinRate() {
            int total = wins + losses;
            if (total == 0) return 0.0;
            return (double) wins / total * 100;
        }
    }
 
    public UserInfo getUserInfo(String username) {
        
        String sql = "SELECT username, email, avatar, score, wins, losses, draws FROM users WHERE username = ?";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                UserInfo user = new UserInfo();
                user.username = rs.getString("username");
                user.email = rs.getString("email");
                user.avatar = rs.getString("avatar");
                if (user.avatar == null || user.avatar.isEmpty()) user.avatar = "1.jpg";
                
                user.elo = rs.getInt("score");
                user.wins = rs.getInt("wins");
                user.losses = rs.getInt("losses");
                user.draws = rs.getInt("draws");
                
                return user;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

   
    public boolean changePassword(String username, String currentPass, String newPass) {
       
        UserInfo user = checkLogin(username, currentPass);
        if (user == null) {
            return false; 
        }
        
        
        String getSaltSql = "SELECT salt FROM users WHERE username = ?";
        String salt = "";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(getSaltSql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) salt = rs.getString("salt");
        } catch (Exception e) { return false; }

        
        String newHash = hashPassword(newPass, salt);
        
        
        String updateSql = "UPDATE users SET password = ? WHERE username = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setString(1, newHash);
            stmt.setString(2, username);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 3. Đổi Email
    public boolean updateEmail(String username, String newEmail) {
        String sql = "UPDATE users SET email = ? WHERE username = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newEmail);
            stmt.setString(2, username);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public boolean updateUsername(String currentUsername, String newUsername) {
        String sql = "UPDATE users SET username = ? WHERE username = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, newUsername);
            stmt.setString(2, currentUsername);
            
            int rowsUpdated = stmt.executeUpdate();
            return rowsUpdated > 0;
            
        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
           
            System.out.println("Tên đăng nhập mới đã tồn tại!");
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
   
}