/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package server;

import database.MatchDAO;
import database.UserDAO;
import org.java_websocket.WebSocket;
import java.util.Map;
public class SurrenderHandler {
    private final MatchDAO matchDAO;
    private final UserDAO userDAO;
    private final Map<String, GameRoom> roomMap;
    private final Map<WebSocket, String> userMap;
    
    public SurrenderHandler(MatchDAO matchDAO, UserDAO userDAO,
                           Map<String, GameRoom> roomMap, Map<WebSocket, String> userMap) {
        this.matchDAO = matchDAO;
        this.userDAO = userDAO;
        this.roomMap = roomMap;
        this.userMap = userMap;
    }
    
    public void handleSurrender(String roomId, String username, WebSocket conn) {
        System.out.println("=== XỬ LÝ ĐẦU HÀNG ===");
        System.out.println(username + " đầu hàng trong phòng " + roomId);
        
        GameRoom room = roomMap.get(roomId);
        if (room == null) {
            System.out.println("Phòng không tồn tại");
            return;
        }
        
        // Tìm đối thủ
        WebSocket opponent = null;
        String opponentUsername = null;
        
        for (WebSocket player : room.players) {
            String playerUsername = userMap.get(player);
            if (playerUsername != null && !playerUsername.equals(username)) {
                opponent = player;
                opponentUsername = playerUsername;
                break;
            }
        }
        
        if (opponent != null && opponentUsername != null) {
            processSurrender(room, username, opponentUsername, conn, opponent);
        } else {
            System.out.println("Không tìm thấy đối thủ");
        }
    }
    
    private void processSurrender(GameRoom room, String surrenderer, String winner,
                                 WebSocket surrendererSocket, WebSocket winnerSocket) {
        // Lưu lịch sử và cập nhật điểm
        matchDAO.saveMatch(winner, surrenderer, room.moveCount);
        userDAO.updateScore(winner, 5, "WIN");
        userDAO.updateScore(surrenderer, -5, "LOSE");
        
        System.out.println("--> Đã lưu history: " + winner + " thắng " + surrenderer + " (Surrender)");
        
        // Gửi thông báo cho cả hai
        String msg = String.format(
            "{\"type\": \"GAME_OVER\", \"winner\": \"%s\", \"reason\": \"SURRENDER\", \"surrenderer\": \"%s\"}",
            winner, surrenderer
        );
        
        System.out.println("Gửi thông báo surrender: " + msg);
        
        // Gửi cho người đầu hàng
        surrendererSocket.send(msg);
        
        // Gửi cho người thắng
        if (winnerSocket != null && winnerSocket.isOpen()) {
            winnerSocket.send(msg);
        }
        
        // Đánh dấu game kết thúc
        room.setGameEnded(true);
        room.stopTimer();
    }
    
    public void handleLeaveAsSurrender(String roomId, String username, WebSocket conn) {
        System.out.println("=== XỬ LÝ RỜI PHÒNG NHƯ ĐẦU HÀNG ===");
        System.out.println(username + " rời phòng " + roomId);
        
        GameRoom room = roomMap.get(roomId);
        if (room == null) {
            return;
        }
        
        if (room.isGameEnded()) {
            System.out.println("Game đã kết thúc, chỉ dọn dẹp phòng");
            return;
        }
        
        // Xóa người chơi này khỏi phòng
        room.players.remove(conn);
        
        // NGƯỜI Ở LẠI THẮNG
        if (!room.players.isEmpty()) {
            WebSocket winnerSocket = room.players.get(0);
            String winnerUsername = userMap.get(winnerSocket);
            
            if (winnerUsername != null) {
                processSurrender(room, username, winnerUsername, conn, winnerSocket);
            }
        }
        
        // Nếu phòng trống thì xóa
        if (room.players.isEmpty()) {
            roomMap.remove(roomId);
        }
    }
}
