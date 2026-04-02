/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package server;

import database.MatchDAO;
import database.UserDAO;
import org.java_websocket.WebSocket;
import java.util.Map;
public class DrawHandler {
    private final MatchDAO matchDAO;
    private final UserDAO userDAO;
    private final Map<String, GameRoom> roomMap;
    private final Map<WebSocket, String> userMap;
    
    public DrawHandler(MatchDAO matchDAO, UserDAO userDAO, 
                      Map<String, GameRoom> roomMap, Map<WebSocket, String> userMap) {
        this.matchDAO = matchDAO;
        this.userDAO = userDAO;
        this.roomMap = roomMap;
        this.userMap = userMap;
    }
    
    public void handleDrawRequest(String roomId, String username, WebSocket conn) {
        System.out.println("=== XỬ LÝ YÊU CẦU HÒA ===");
        System.out.println(username + " xin hòa trong phòng " + roomId);
        
        GameRoom room = roomMap.get(roomId);
        if (room == null || room.isGameEnded()) {
            System.out.println("Phòng không tồn tại hoặc game đã kết thúc");
            return;
        }
        
       
        if (room.isBotMode) {
             conn.send("{\"type\": \"CHAT\", \"sender\": \"Hệ thống\", \"text\": \"Không thể xin hòa với máy!\"}");
             return;
        }

       
        WebSocket opponent = null;
        String opponentUsername = null;
        
        for (WebSocket player : room.players) {
            String pName = userMap.get(player);
           
            if (pName != null && !pName.equals(username)) {
                opponent = player;
                opponentUsername = pName;
                break;
            }
        }
        
        if (opponent != null) {
            
            String requestMsg = String.format(
                "{\"type\": \"DRAW_REQUEST\", \"sender\": \"%s\", \"roomId\": \"%s\"}", 
                username, roomId
            );
            System.out.println("Gửi tin nhắn xin hòa đến: " + opponentUsername);
            opponent.send(requestMsg);
            
         
            String confirmMsg = "{\"type\": \"DRAW_REQUEST_SENT\", \"message\": \"Đã gửi lời mời hòa!\"}";
            conn.send(confirmMsg);
        } else {
            System.out.println("LỖI: Không tìm thấy đối thủ trong phòng!");
        }
    }
    
    public void handleDrawResponse(String roomId, String username, String accept, WebSocket conn) {
        System.out.println("=== XỬ LÝ PHẢN HỒI HÒA ===");
        System.out.println(username + " trả lời: " + accept);
        
        GameRoom room = roomMap.get(roomId);
        if (room == null || room.isGameEnded()) {
            System.out.println("Phòng không tồn tại hoặc game đã kết thúc");
            return;
        }
        
        // Tìm người gửi yêu cầu
        WebSocket requestSender = null;
        String requestSenderUsername = null;
        
        for (WebSocket player : room.players) {
            String playerUsername = userMap.get(player);
            if (playerUsername != null && !playerUsername.equals(username)) {
                requestSender = player;
                requestSenderUsername = playerUsername;
                break;
            }
        }
        
        if (requestSender != null && requestSenderUsername != null) {
            if ("true".equals(accept)) {
                handleDrawAccepted(room, requestSenderUsername, username, requestSender, conn);
            } else {
                handleDrawRejected(username, requestSender);
                conn.send("{\"type\": \"DRAW_RESPONSE_SENT\", \"message\": \"Đã từ chối lời mời hòa\"}");
            }
        }
    }
    
    private void handleDrawAccepted(GameRoom room, String sender, String responder, 
                                   WebSocket senderSocket, WebSocket responderSocket) {
        System.out.println("Cả hai đồng ý hòa!");
        
        room.stopTimer();
        room.setGameEnded(true);
        
        // Lưu lịch sử trận hòa
        matchDAO.saveDrawMatch(sender, responder, room.moveCount);
        
        // Cập nhật điểm
        userDAO.updateScore(sender, 2, "DRAW");
        userDAO.updateScore(responder, 2, "DRAW");
        
        // Gửi thông báo HÒA cho cả hai
        String drawMsg = String.format(
            "{\"type\": \"GAME_OVER\", \"result\": \"DRAW\", \"reason\": \"AGREEMENT\", \"message\": \"Cả hai đồng ý hòa!\"}"
        );
        
        senderSocket.send(drawMsg);
        responderSocket.send(drawMsg);
        
        System.out.println("Đã xử lý hòa thành công!");
    }
    
    private void handleDrawRejected(String rejecter, WebSocket requestSender) {
        System.out.println(rejecter + " từ chối hòa");
        
        String rejectMsg = String.format(
            "{\"type\": \"DRAW_REJECTED\", \"rejecter\": \"%s\"}", 
            rejecter
        );
        requestSender.send(rejectMsg);
    }
}
