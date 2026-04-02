/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package server;

import database.UserDAO;
import database.UserDAO.UserInfo;
import org.java_websocket.WebSocket;

import java.util.Map;
public class ProfileHandler {
    private final UserDAO userDAO;
    private final Map<String, String> avatarCache;
private final Map<WebSocket, String> userMap;

    
    public ProfileHandler(UserDAO userDAO, Map<String, String> avatarCache, Map<WebSocket, String> userMap) {
        this.userDAO = userDAO;
        this.avatarCache = avatarCache;
        this.userMap = userMap; 
    }

    public void handleGetUserInfo(String username, WebSocket conn) {
        UserInfo userInfo = userDAO.getUserInfo(username);
        
        if (userInfo != null) {
            
            String cachedAvatar = avatarCache.getOrDefault(username, userInfo.avatar);
            
            String response = String.format(
                "{\"type\": \"USER_INFO\", \"username\": \"%s\", \"email\": \"%s\", \"avatar\": \"%s\", \"elo\": %d, \"wins\": %d, \"losses\": %d, \"draws\": %d, \"joinDate\": \"%s\"}",
                userInfo.username, 
                userInfo.email, 
                cachedAvatar, 
                userInfo.elo, 
                userInfo.wins, 
                userInfo.losses, 
                userInfo.draws, 
                "2024-01-01" 
            );
            conn.send(response);
            
            
            String statsResponse = String.format(
                "{\"type\": \"STATS_DATA\", \"wins\": %d, \"losses\": %d, \"draws\": %d, \"elo\": %d}",
                userInfo.wins, userInfo.losses, userInfo.draws, userInfo.elo
            );
            conn.send(statsResponse);
        } else {
             conn.send("{\"type\": \"ERROR\", \"message\": \"Không tìm thấy thông tin người dùng!\"}");
        }
    }

    public void handleChangePassword(String username, String currentPass, String newPass, WebSocket conn) {
        
        boolean success = userDAO.changePassword(username, currentPass, newPass);
        
        if (success) {
            conn.send("{\"type\": \"PASSWORD_CHANGED\", \"success\": true}");
        } else {
            conn.send("{\"type\": \"PASSWORD_CHANGED\", \"success\": false, \"message\": \"Mật khẩu hiện tại không đúng!\"}");
        }
    }

    public void handleChangeEmail(String username, String newEmail, WebSocket conn) {
        boolean success = userDAO.updateEmail(username, newEmail);
        if (success) {
            String response = String.format("{\"type\": \"EMAIL_CHANGED\", \"success\": true, \"newEmail\": \"%s\"}", newEmail);
            conn.send(response);
        } else {
            conn.send("{\"type\": \"EMAIL_CHANGED\", \"success\": false, \"message\": \"Email không hợp lệ hoặc đã tồn tại!\"}");
        }
    }
    
    public void handleGetEloHistory(String username, String range, WebSocket conn) {
       
        String response = "{\"type\": \"ELO_HISTORY\", \"history\": []}"; 
        conn.send(response);
    }
    public void handleChangeUsername(String currentUsername, String newUsername, WebSocket conn) {
        
        if (newUsername == null || newUsername.trim().length() < 3) {
            conn.send("{\"type\": \"USERNAME_CHANGED\", \"success\": false, \"message\": \"Tên phải từ 3 ký tự trở lên!\"}");
            return;
        }

        
        boolean success = userDAO.updateUsername(currentUsername, newUsername);

        if (success) {
            
            
            
            userMap.put(conn, newUsername);
            
            
            if (avatarCache.containsKey(currentUsername)) {
                String currentAvatar = avatarCache.get(currentUsername);
                avatarCache.remove(currentUsername);
                avatarCache.put(newUsername, currentAvatar);
            }

            
            String response = String.format("{\"type\": \"USERNAME_CHANGED\", \"success\": true, \"newUsername\": \"%s\"}", newUsername);
            conn.send(response);
            System.out.println("User " + currentUsername + " đã đổi tên thành " + newUsername);
            
        } else {
            conn.send("{\"type\": \"USERNAME_CHANGED\", \"success\": false, \"message\": \"Tên đăng nhập đã tồn tại hoặc lỗi hệ thống!\"}");
        }
    }
}
