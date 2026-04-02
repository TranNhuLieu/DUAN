/* File: server/GameRoom.java */
package server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Stack;
import org.java_websocket.WebSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class GameRoom {
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> currentTimer;
    private final int TIME_LIMIT = 60;
     String roomId;
    List<WebSocket> players = new ArrayList<>();
    Map<WebSocket, String> playerUsernames = new HashMap<>();
    private int[][] board = new int[15][15];
    private String currentTurn = "X"; // Mặc định X đi trước
    private volatile boolean gameEnded = false;
    private String winner = null;
    public int moveCount = 0;
    public boolean isBotMode = false;
    private Stack<int[]> moveHistory = new Stack<>();
    
    public GameRoom(String id) {
        this.roomId = id;
    }
    public String getCurrentTurn() {
        return currentTurn;
    }
    public boolean isGameEnded() {
        return gameEnded;
    }
    
    public void setGameEnded(boolean ended) {
        this.gameEnded = ended;
        if (ended) {
            stopTimer(); 
        }
    }
    public void startTimer() {
        // Hủy timer cũ nếu đang chạy
        stopTimer();

        System.out.println("Bắt đầu đếm ngược " + TIME_LIMIT + "s cho lượt: " + currentTurn);

        // Tạo timer mới
        currentTimer = scheduler.schedule(() -> {
            handleTimeout();
        }, TIME_LIMIT, TimeUnit.SECONDS);
    }

    // Hàm dừng timer
    public void stopTimer() {
        if (currentTimer != null) {
            
            currentTimer.cancel(true); 
            currentTimer = null;
        }
    }

    
    private void handleTimeout() {
        if (gameEnded) return;

    System.out.println("HẾT GIỜ! Không ai đánh trong 30 giây.");
    gameEnded = true;
    
 
    boolean boardAlmostFull = true;
    int emptyCount = 0;
    
    for (int r = 0; r < 15; r++) {
        for (int c = 0; c < 15; c++) {
            if (board[r][c] == 0) {
                emptyCount++;
                if (emptyCount > 10) { 
                    boardAlmostFull = false;
                    break;
                }
            }
        }
        if (!boardAlmostFull) break;
    }
    
    if (boardAlmostFull && emptyCount > 0) {
       
        System.out.println("Trận đấu hòa do hết thời gian và bàn cờ gần đầy");
        String drawMsg = "{\"type\": \"GAME_OVER\", \"result\": \"DRAW\", \"reason\": \"TIME_OUT_DRAW\"}";
        broadcast(drawMsg);
    } else {
        // Xử lý như cũ: người đến lượt thua
        System.out.println("Người chơi " + currentTurn + " bị xử thua do hết giờ.");
        
        String winnerSymbol = currentTurn.equals("X") ? "O" : "X";
        
        // Tìm username của người thắng
        String winnerUsername = null;
        // Logic tìm người thắng...
        
        String msg = String.format(
            "{\"type\": \"GAME_OVER\", \"winner\": \"%s\", \"reason\": \"TIMEOUT\"}", 
            winnerUsername != null ? winnerUsername : winnerSymbol
        );
        broadcast(msg);
    }
    }

    
    private void broadcast(String msg) {
        for (WebSocket player : players) {
            if (player.isOpen()) {
                player.send(msg);
            }
        }
    }
    public int makeMove(int row, int col, String symbol) {
    System.out.println("=== DEBUG makeMove ===");
    System.out.println("Current turn: " + currentTurn);
    System.out.println("Player symbol: " + symbol);
    
    if (gameEnded) {
        System.out.println("Game đã kết thúc");
        return 0;
    }
    
    // Kiểm tra lượt
    if (!symbol.equals(currentTurn)) {
        System.out.println("Sai lượt! Expected: " + currentTurn + ", Got: " + symbol);
        return 0;
    }
    
    // Kiểm tra tọa độ
    if (row < 0 || row >= 15 || col < 0 || col >= 15) {
        System.out.println("Tọa độ ngoài bàn cờ");
        return 0;
    }
    
    // Kiểm tra ô trống
    if (board[row][col] != 0) {
        System.out.println("Ô đã có quân");
        return 0;
    }
    
    // Thực hiện nước đi
    int playerValue = symbol.equals("X") ? 1 : 2;
    board[row][col] = playerValue;
    moveCount++;
    
    // Lưu vào history để undo
    if (moveHistory == null) {
        moveHistory = new Stack<>();
    }
    moveHistory.push(new int[]{row, col, playerValue});
    
    System.out.println("Đánh thành công tại [" + row + "," + col + "]");
    
    boolean isWin = checkWin(row, col, playerValue);
        if (isWin) {
            setGameEnded(true); 
            winner = symbol;
            System.out.println("Chiến thắng! Winner: " + symbol);
            return 2; 
        }
    
    // Kiểm tra bàn cờ đầy (HÒA)
    boolean isBoardFull = true;
    for (int r = 0; r < 15; r++) {
        for (int c = 0; c < 15; c++) {
            if (board[r][c] == 0) {
                isBoardFull = false;
                break;
            }
        }
        if (!isBoardFull) break;
    }
    
    if (isBoardFull) {
            setGameEnded(true); 
            System.out.println("Bàn cờ đầy - HÒA!");
            return 3; 
        }
    
    // Đổi lượt
    currentTurn = currentTurn.equals("X") ? "O" : "X";
    
    // Timer logic
    if (!isBotMode) { 
        startTimer();
    } else if (currentTurn.equals("X")) {
        startTimer();
    } else {
        stopTimer();
    }
    
    return 1; // MOVE OK
}
    
    public void makeBotMove() {
        System.out.println("=== BOT BẮT ĐẦU SUY NGHĨ ===");
        System.out.println("Current turn trước khi bot đánh: " + currentTurn);

        if (gameEnded) {
            System.out.println("Game đã kết thúc, bot không đánh");
            return;
        }
       
        try { 
            Thread.sleep(500 + new Random().nextInt(500)); 
        } catch (InterruptedException e) {}
       
        int[] attackMove = findWinningMove(2); 
        if (attackMove != null) {
            System.out.println("Bot phát hiện cơ hội THẮNG tại [" + attackMove[0] + "," + attackMove[1] + "]");
            executeBotMove(attackMove[0], attackMove[1]);
            return;
        }
        
        // Ưu tiên 2: Phòng thủ - chặn người chơi có 4 quân liên tiếp
        int[] defenseMove = findWinningMove(1); 
        if (defenseMove != null) {
            System.out.println("Bot phát hiện NGUY HIỂM, chặn tại [" + defenseMove[0] + "," + defenseMove[1] + "]");
            executeBotMove(defenseMove[0], defenseMove[1]);
            return;
        }
       
        int[] smartMove = findSmartMove();
        if (smartMove != null) {
            System.out.println("Bot đánh nước thông minh tại [" + smartMove[0] + "," + smartMove[1] + "]");
            executeBotMove(smartMove[0], smartMove[1]);
            return;
        }
        
        int[] randomMove = findRandomMove();
        if (randomMove != null) {
            System.out.println("Bot đánh ngẫu nhiên tại [" + randomMove[0] + "," + randomMove[1] + "]");
            executeBotMove(randomMove[0], randomMove[1]);
            return;
        }
        
        System.out.println("Bot không tìm được nước đi!");
    }
    
    private int[] findWinningMove(int player) {
        
        for (int r = 0; r < 15; r++) {
            for (int c = 0; c < 15; c++) {
                if (board[r][c] == 0) {
                    // Thử đánh vào ô này
                    board[r][c] = player;
                    if (checkWin(r, c, player)) {
                        board[r][c] = 0; // Hoàn tác
                        return new int[]{r, c};
                    }
                    board[r][c] = 0; // Hoàn tác
                }
            }
        }
        return null;
    }
    
    private int[] findSmartMove() {
        
        List<int[]> goodMoves = new ArrayList<>();
        
        for (int r = 0; r < 15; r++) {
            for (int c = 0; c < 15; c++) {
                if (board[r][c] == 0) {
                    int score = evaluatePosition(r, c);
                    if (score > 0) {
                        goodMoves.add(new int[]{r, c, score});
                    }
                }
            }
        }
        
        if (!goodMoves.isEmpty()) {
           
            goodMoves.sort((a, b) -> Integer.compare(b[2], a[2]));
            return new int[]{goodMoves.get(0)[0], goodMoves.get(0)[1]};
        }
        
        return null;
    }
    
    private int evaluatePosition(int row, int col) {
        int score = 0;
        
        int centerRow = 7, centerCol = 7;
        int distanceFromCenter = Math.abs(row - centerRow) + Math.abs(col - centerCol);
        score += (14 - distanceFromCenter) * 2;
      
        for (int r = Math.max(0, row-2); r <= Math.min(14, row+2); r++) {
            for (int c = Math.max(0, col-2); c <= Math.min(14, col+2); c++) {
                if (board[r][c] == 2) { 
                    score += 5;
                }
                if (board[r][c] == 1) { 
                    score += 3; 
                }
            }
        }
        
        return score;
    }
    
    private int[] findRandomMove() {
        List<int[]> emptyCells = new ArrayList<>();
        
        for (int r = 5; r <= 9; r++) {
            for (int c = 5; c <= 9; c++) {
                if (board[r][c] == 0) {
                    emptyCells.add(new int[]{r, c});
                }
            }
        }
        
        if (!emptyCells.isEmpty()) {
            return emptyCells.get(new Random().nextInt(emptyCells.size()));
        }
       
        for (int r = 0; r < 15; r++) {
            for (int c = 0; c < 15; c++) {
                if (board[r][c] == 0) {
                    emptyCells.add(new int[]{r, c});
                }
            }
        }
        
        if (!emptyCells.isEmpty()) {
            return emptyCells.get(new Random().nextInt(emptyCells.size()));
        }
        
        return null;
    }
    
    private void executeBotMove(int row, int col) {
      
        board[row][col] = 2;
        this.moveCount++;
        
        System.out.println("Bot đánh tại: [" + row + "," + col + "]");
        
      
        String botMoveMsg = String.format(
            "{\"type\": \"MOVE\", \"roomId\": \"%s\", \"row\": %d, \"col\": %d, \"symbol\": \"O\", \"username\": \"BOT_SIEU_CAP\"}",
            roomId, row, col
        );
        

        if (!players.isEmpty()) {
            players.get(0).send(botMoveMsg);
        }
        
      
        if (checkWin(row, col, 2)) {
            gameEnded = true;
            System.out.println("BOT CHIẾN THẮNG!");
            
            // Gửi thông báo thắng
            String winMsg = "{\"type\": \"GAME_OVER\", \"winner\": \"BOT_SIEU_CAP\"}";
            if (!players.isEmpty()) {
                players.get(0).send(winMsg);
            }
        } else {
            // Đổi lượt lại cho người chơi
            currentTurn = "X";
            System.out.println("Đổi lượt về cho người chơi (X)");
        }
    }
    
    private boolean checkWin(int row, int col, int player) {
        int[] dRow = {0, 1, 1, 1};
        int[] dCol = {1, 0, 1, -1};
        for (int i = 0; i < 4; i++) {
            int count = 1; 
            count += countDirection(row, col, dRow[i], dCol[i], player);  
            count += countDirection(row, col, -dRow[i], -dCol[i], player); 
            if (count >= 5) return true; 
        }
        return false;
    }

    private int countDirection(int row, int col, int dRow, int dCol, int player) {
        int count = 0;
        int r = row + dRow;
        int c = col + dCol;
        while (r >= 0 && r < 15 && c >= 0 && c < 15 && board[r][c] == player) {
            count++;
            r += dRow;
            c += dCol;
        }
        return count;
    }
    public void undoLastMove() {
        if (!moveHistory.isEmpty()) {
            int[] lastMove = moveHistory.pop();
            int r = lastMove[0];
            int c = lastMove[1];
            board[r][c] = 0; // Reset ô đó về 0
            moveCount--;
            
            // Đổi lại lượt
            currentTurn = currentTurn.equals("X") ? "O" : "X";
            
            // Reset timer cho lượt cũ
            startTimer();
        }
    }
    public void saveDrawMatch(String player1, String player2, int moves) {
    
    System.out.println("Lưu lịch sử trận hòa: " + player1 + " vs " + player2 + " với " + moves + " nước");
}

}