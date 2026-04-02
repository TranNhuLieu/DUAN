let countdownInterval;
document.addEventListener('DOMContentLoaded', function() {
    
    const user = JSON.parse(localStorage.getItem('user'));
    let roomInfo = JSON.parse(localStorage.getItem('currentRoom'));
    
    if (!user || !roomInfo) {
        alert('Lỗi thông tin phòng! Quay về sảnh.');
        window.location.href = 'lobby.html';
        return;
    }
    
   
   
    if (roomInfo.yourSymbol === 'X') {
        setupUI(user.username, roomInfo.opponent, 'X', 'O');
    } else {
        setupUI(roomInfo.opponent, user.username, 'X', 'O');
    }

    function setupUI(p1Name, p2Name, p1Sym, p2Sym) {
        document.getElementById('player1Name').textContent = p1Name;
        document.getElementById('player2Name').textContent = p2Name;
        startClientTimer(60);

        if (p2Name === "BOT_SIEU_CAP") {
            document.querySelector('.player-2').classList.add('player-bot');
        }
        
       
        updateTurnInfo(p1Name, 'X'); 
    }
function startClientTimer(duration = 30) {
    const timerElement = document.getElementById('timer');
    let timeLeft = duration;
    
    
    if (countdownInterval) clearInterval(countdownInterval); 
    
   
    timerElement.innerText = timeLeft + "s";
    timerElement.style.color = "var(--neon-blue)"; 

    countdownInterval = setInterval(() => {
        timeLeft--;
        timerElement.innerText = timeLeft + "s";

        
        if (timeLeft <= 5) {
            timerElement.style.color = "#ff0055"; 
            timerElement.style.textShadow = "0 0 10px #ff0055"; 
        }

        if (timeLeft <= 0) {
            clearInterval(countdownInterval);
            timerElement.innerText = "0s";
        }
    }, 1000);
}
wsService.on('MATCH_FOUND', function(data) {
    console.log("Nhận MATCH_FOUND từ server:", data);
    
    // Lưu thông tin phòng mới
    localStorage.setItem('currentRoom', JSON.stringify({
        roomId: data.roomId,
        opponent: data.opponent,
        yourSymbol: data.yourSymbol
    }));
    
    // Reload trang để vào game mới
    setTimeout(() => {
        window.location.reload();
    }, 500);
});
// Trong xử lý MOVE từ Bot
wsService.on('MOVE', function(data) {
    console.log("Nhận MOVE:", data);
    
    const cell = document.getElementById(`cell-${data.row}-${data.col}`);
    
    if (cell && !cell.textContent) {
        // Vẽ quân cờ
        cell.textContent = data.symbol;
        cell.className = `cell-content ${data.symbol.toLowerCase()}`;
        
    
        if (data.username === "BOT_SIEU_CAP") {
            cell.classList.add('bot-move');
        }
        
        // Hiệu ứng
       cell.style.animation = 'placePiece 0.3s ease-out';
    
  
    const nextTurnSymbol = data.symbol === 'X' ? 'O' : 'X';
    let nextPlayerName = '';
    
    if (roomInfo.yourSymbol === nextTurnSymbol) {
        nextPlayerName = user.username; 
    } else {
        nextPlayerName = roomInfo.opponent; 
    }
    
  
    updateTurnInfo(nextPlayerName, nextTurnSymbol);
    
    
    updateMovesList(data);
    startClientTimer(60);
    }
});
    
   
    setTimeout(() => { document.getElementById('loading').style.display = 'none'; }, 500);
    
   
    initializeBoard();
    

    
    if(wsService.socket && wsService.socket.readyState === WebSocket.OPEN) {
        joinGame();
    } else {
       
        setTimeout(joinGame, 1000);
    }

    function joinGame() {
        console.log("Gửi lệnh JOIN_GAME...");
        wsService.send({
            type: 'JOIN_GAME', 
            roomId: roomInfo.roomId,
            username: user.username
        });
    }

    
    window.handleCellClick = function(row, col) {
        const cell = document.getElementById(`cell-${row}-${col}`);
        if (cell.textContent !== '') return; 

        
        wsService.send({
            type: 'MOVE', 
            roomId: roomInfo.roomId,
            row: row,
            col: col,
            symbol: roomInfo.yourSymbol, 
            username: user.username
        });
    };

   
    wsService.on('MOVE', function(data) {
    console.log("Nhận MOVE từ server:", data);
    
    const cell = document.getElementById(`cell-${data.row}-${data.col}`);
    
    if (cell && !cell.textContent) {
        console.log("Vẽ quân cờ tại:", data.row, data.col, "symbol:", data.symbol);
        
       
        cell.textContent = data.symbol;
        cell.className = `cell-content ${data.symbol.toLowerCase()}`;
        
       
        if (data.username === "BOT_SIEU_CAP") {
            console.log("Đây là nước đi của Bot!");
            cell.classList.add('bot-move');
        } 
            
            
            cell.style.animation = 'placePiece 0.3s ease-out';
            
           
            const nextTurnSymbol = data.symbol === 'X' ? 'O' : 'X';
            
            
            let nextPlayerName = '';
            if (roomInfo.yourSymbol === nextTurnSymbol) {
                nextPlayerName = user.username; 
            } else {
                nextPlayerName = roomInfo.opponent;
            }
            
            document.getElementById('currentPlayer').textContent = nextPlayerName;
            
            
            updateMovesList(data);
        }
    });
wsService.on('GAME_OVER', (data) => {
    clearInterval(countdownInterval);
    console.log("Nhận GAME_OVER từ server:", data);
    
    let title = "";
    let message = "";
    let resultType = "";
    
   
    if (data.result === "DRAW") {
        title = "HÒA!";
        message = data.message || "Trận đấu kết thúc với tỷ số hòa!";
        resultType = "draw";
        
        // Cập nhật stats
        updateUserStats("draw");
        
    } else if (data.reason === "BOARD_FULL") {
        title = "HÒA!";
        message = "Bàn cờ đã đầy. Trận đấu hòa!";
        resultType = "draw";
        updateUserStats("draw");
        
    } else if (data.reason === "TIME_OUT_DRAW") {
        title = "HÒA!";
        message = "Hết thời gian và bàn cờ gần đầy. Trận hòa!";
        resultType = "draw";
        updateUserStats("draw");
        
    } else if (data.reason === "OPPONENT_DISCONNECTED") {
        title = "CHIẾN THẮNG!";
        message = "Đối thủ đã thoát trận. Bạn được tính thắng!";
        resultType = "win";
        updateUserStats("win");
        
    } else if (data.reason === "SURRENDER") {
        if (data.surrenderer === user.username) {
            title = "THẤT BẠI";
            message = "Bạn đã đầu hàng!";
            resultType = "lose";
            updateUserStats("lose");
        } else {
            title = "CHIẾN THẮNG!";
            message = "Đối thủ đã đầu hàng!";
            resultType = "win";
            updateUserStats("win");
        }
        
    } else if (data.reason === "TIMEOUT") {
        if (data.winner === user.username) {
            title = "CHIẾN THẮNG!";
            message = "Đối thủ hết thời gian!";
            resultType = "win";
            updateUserStats("win");
        } else {
            title = "THẤT BẠI";
            message = "Bạn đã hết thời gian!";
            resultType = "lose";
            updateUserStats("lose");
        }
        
    } else {
        // Thắng/thua thông thường
        const isMeWinner = (data.winner === user.username);
        if (isMeWinner) {
            title = "CHIẾN THẮNG!";
            message = "Bạn đã tạo được 5 quân liên tiếp!";
            resultType = "win";
            updateUserStats("win");
        } else {
            title = "THẤT BẠI";
            message = data.winner + " đã tạo được 5 quân liên tiếp!";
            resultType = "lose";
            updateUserStats("lose");
        }
    }
    

    showVictoryScreen(title, message, resultType);
});
function addStatsButton() {
    const victoryContent = document.querySelector('.victory-content');
    if (victoryContent) {
        const statsBtn = document.createElement('button');
        statsBtn.className = 'game-btn btn-info';
        statsBtn.innerHTML = '<i class="fas fa-chart-bar"></i> Xem thống kê';
        statsBtn.onclick = function() {
            showStatsModal();
        };
        victoryContent.appendChild(statsBtn);
    }
}

// Modal hiển thị thống kê chi tiết
function showStatsModal() {
    const stats = JSON.parse(localStorage.getItem('userStats')) || {
        wins: 0,
        losses: 0,
        draws: 0,
        score: 1000
    };
    
    const totalGames = stats.wins + stats.losses + stats.draws;
    const winRate = totalGames > 0 ? Math.round((stats.wins / totalGames) * 100) : 0;
    const drawRate = totalGames > 0 ? Math.round((stats.draws / totalGames) * 100) : 0;
    
    const modalHTML = `
        <div class="stats-modal">
            <div class="stats-header">
                <h3>THỐNG KÊ CÁ NHÂN</h3>
                <button onclick="closeStatsModal()" class="close-btn">&times;</button>
            </div>
            <div class="stats-body">
                <div class="stat-row">
                    <span>Số trận:</span>
                    <strong>${totalGames}</strong>
                </div>
                <div class="stat-row">
                    <span>Thắng:</span>
                    <strong style="color: #00ff88">${stats.wins} (${winRate}%)</strong>
                </div>
                <div class="stat-row">
                    <span>Thua:</span>
                    <strong style="color: #ff5555">${stats.losses}</strong>
                </div>
                <div class="stat-row">
                    <span>Hòa:</span>
                    <strong style="color: #ffdd00">${stats.draws} (${drawRate}%)</strong>
                </div>
                <div class="stat-row">
                    <span>Điểm:</span>
                    <strong style="color: #00d4ff">${stats.score}</strong>
                </div>
            </div>
        </div>
    `;
    
    // Tạo và hiển thị modal
    const modal = document.createElement('div');
    modal.id = 'statsModal';
    modal.innerHTML = modalHTML;
    document.body.appendChild(modal);
}

function closeStatsModal() {
    const modal = document.getElementById('statsModal');
    if (modal) {
        modal.remove();
    }
}

// CSS cho modal
const style = document.createElement('style');
style.textContent = `
    #statsModal {
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0,0,0,0.8);
        display: flex;
        justify-content: center;
        align-items: center;
        z-index: 9999;
    }
    
    .stats-modal {
        background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
        border: 2px solid var(--neon-blue);
        border-radius: 15px;
        padding: 25px;
        width: 350px;
        max-width: 90%;
        box-shadow: 0 0 30px rgba(0, 212, 255, 0.5);
    }
    
    .stats-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 20px;
        border-bottom: 1px solid rgba(0, 212, 255, 0.3);
        padding-bottom: 10px;
    }
    
    .stats-header h3 {
        color: var(--neon-blue);
        margin: 0;
        font-family: 'Orbitron';
    }
    
    .close-btn {
        background: none;
        border: none;
        color: white;
        font-size: 24px;
        cursor: pointer;
        padding: 0 10px;
    }
    
    .stat-row {
        display: flex;
        justify-content: space-between;
        padding: 10px 0;
        border-bottom: 1px solid rgba(255,255,255,0.1);
        font-family: 'Orbitron';
    }
    
    .stat-row:last-child {
        border-bottom: none;
    }
`;
document.head.appendChild(style);
document.getElementById('btnDraw').addEventListener('click', function() {
    Swal.fire({
        title: 'Xin hòa?',
        text: "Gửi lời mời hòa đến đối thủ",
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#3085d6',
        cancelButtonColor: '#d33',
        confirmButtonText: 'Gửi yêu cầu',
        cancelButtonText: 'Hủy'
    }).then((result) => {
        if (result.isConfirmed) {
            wsService.send({
                type: 'DRAW_REQUEST',
                roomId: roomInfo.roomId,
                username: user.username
            });
            
            Swal.fire({
                title: 'Đã gửi!',
                text: 'Đang chờ đối thủ phản hồi...',
                icon: 'info',
                timer: 2000,
                showConfirmButton: false
            });
        }
    });
});

// 2. Nhận yêu cầu hòa từ đối thủ
wsService.on('DRAW_REQUEST', function(data) {
    console.log("Nhận yêu cầu hòa từ:", data.sender);
    
 
    showRequestModal(
        'Yêu cầu hòa!', 
        `${data.sender} muốn cầu hòa. Bạn có đồng ý không?`,
        function() {
         
            console.log("Chấp nhận hòa");
            wsService.send({
                type: 'DRAW_RESPONSE',
                roomId: roomInfo.roomId,
                username: user.username,
                accept: "true"
            });
        },
        function() {
          
            console.log("Từ chối hòa");
            wsService.send({
                type: 'DRAW_RESPONSE',
                roomId: roomInfo.roomId,
                username: user.username,
                accept: "false"
            });
        }
    );
});


wsService.on('DRAW_REJECTED', function(data) {
    Swal.fire({
        title: 'Bị từ chối',
        text: `Đối thủ ${data.rejecter} đã từ chối lời mời hòa!`,
        icon: 'error',
        timer: 2000,
        showConfirmButton: false
    });
});
function showVictoryScreen(title, message, resultType) {
    document.getElementById('victoryTitle').textContent = title;
    document.getElementById('victoryMessage').textContent = message;
    
    const overlay = document.getElementById('victoryOverlay');
    overlay.className = `victory-overlay ${resultType}`; 
    overlay.style.display = 'flex';
    
    if (resultType === "draw") {
        document.getElementById('victoryTitle').style.color = "#FFD700"; 
    }
    
    window.gameOver = true;
    
    
    // localStorage.removeItem('currentRoom');

    if (resultType === "win") {
        updateUserStats("win");
    } else if (resultType === "lose") {
        updateUserStats("lose");
    } else if (resultType === "draw") {
        updateUserStats("draw");
    }
    
 
    const victoryContent = document.querySelector('.victory-content');
    victoryContent.innerHTML = `
        <h1 class="victory-title" id="victoryTitle">${title}</h1>
        <div class="victory-message" id="victoryMessage">${message}</div>
    `;
    
    
    if (roomInfo.opponent !== "BOT_SIEU_CAP") {
        const rematchBtn = document.createElement('button');
        rematchBtn.id = 'btnRematch'
        rematchBtn.className = 'game-btn btn-success';
        rematchBtn.innerHTML = '<i class="fas fa-redo"></i> CHƠI LẠI';
        rematchBtn.onclick = function() {
            sendRematchRequest();
        };
        victoryContent.appendChild(rematchBtn);
    }
    
   
    const botBtn = document.createElement('button');
    botBtn.className = 'game-btn btn-primary';
    botBtn.innerHTML = '<i class="fas fa-robot"></i> CHƠI VỚI BOT';
    botBtn.onclick = function() {
        document.getElementById('loading').style.display = 'flex';
        wsService.send({
            
            type: 'PLAY_WITH_BOT',
            username: user.username
        });
    };
    victoryContent.appendChild(botBtn);
    
  
    const lobbyBtn = document.createElement('button');
    lobbyBtn.className = 'game-btn btn-secondary';
    lobbyBtn.innerHTML = '<i class="fas fa-home"></i> VỀ PHÒNG CHỜ';
    lobbyBtn.onclick = function() {
        window.goToLobby();
    };
    victoryContent.appendChild(lobbyBtn);
}
function sendRematchRequest() {
    if (roomInfo.opponent === "BOT_SIEU_CAP") {
        alert("Không thể chơi lại với Bot!");
        return;
    }
    
    // 1. Gửi tin nhắn lên server
    wsService.send({
        type: 'REMATCH_REQUEST',
        roomId: roomInfo.roomId,
        username: user.username
    });


    const rematchBtn = document.getElementById('btnRematch'); 
    if (rematchBtn) {
        rematchBtn.textContent = "Đang chờ đối thủ...";
        rematchBtn.disabled = true;
    }
    Swal.fire({
        title: 'Đã gửi lời mời!',
        text: 'Đang chờ đối thủ trả lời...',
        icon: 'success',
        timer: 2000,
        showConfirmButton: false
    });
}
let currentRequestData = null;


function showRequestModal(title, message, onAccept, onDecline) {
    Swal.fire({
        title: title,
        text: message,
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#3085d6',
        cancelButtonColor: '#d33',
        confirmButtonText: 'Đồng ý',
        cancelButtonText: 'Từ chối'
    }).then((result) => {
        if (result.isConfirmed) {
            if (onAccept) onAccept();
        } else {
            if (onDecline) onDecline();
        }
    });
}
// Xử lý yêu cầu chơi lại từ đối thủ
wsService.on('REMATCH_REQUEST', function(data) {
    Swal.fire({
        title: 'Thách đấu lại!',
        text: "Đối thủ muốn chơi ván nữa. Bạn dám không?",
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#3085d6',
        cancelButtonColor: '#d33',
        confirmButtonText: 'Chiến luôn!',
        cancelButtonText: 'Sợ rồi'
    }).then((result) => {
        if (result.isConfirmed) {
            // Đồng ý
            wsService.send({
                type: 'REMATCH_RESPONSE',
                roomId: roomInfo.roomId,
                username: user.username,
                accept: "true"
            });
        } else {
            // Từ chối
            wsService.send({
                type: 'REMATCH_RESPONSE',
                roomId: roomInfo.roomId,
                username: user.username,
                accept: "false"
            });
        }
    });
});
// Nhận phản hồi đồng ý chơi lại
wsService.on('REMATCH_ACCEPTED', function(data) {
    console.log("Ván mới bắt đầu:", data);
    
    // 1. Cập nhật thông tin phòng mới vào biến toàn cục
    const newRoomInfo = {
        roomId: data.roomId,
        opponent: data.opponent,
        yourSymbol: data.yourSymbol
    };
    
    // Cập nhật cả vào localStorage và biến đang chạy
    localStorage.setItem('currentRoom', JSON.stringify(newRoomInfo));
    roomInfo = newRoomInfo;

    // 2. Gọi hàm Reset giao diện (Không reload trang)
    resetGameUI(newRoomInfo);
    
    // 3. Thông báo nhẹ (hoặc dùng Toast nếu có)
    // alert("Ván mới bắt đầu!"); // Có thể bỏ dòng này nếu thấy phiền // Hoặc dùng thư viện SweetAlert nếu có

    // --- QUAN TRỌNG: Gửi lệnh tham gia phòng mới ngay lập tức ---
    wsService.send({
        type: 'JOIN_GAME', 
        roomId: data.roomId,
        username: user.username
    });

    // --- QUAN TRỌNG: Reset giao diện mà KHÔNG reload trang ---
    resetGameUI(newRoomInfo);
});
// Nhận phản hồi từ chối chơi lại
wsService.on('REMATCH_REJECTED', function(data) {
    alert("Đối thủ đã từ chối lời mời chơi lại!");
});

// Nhận xác nhận đã gửi yêu cầu
wsService.on('REMATCH_REQUEST_SENT', function(data) {
    console.log("Đã gửi yêu cầu chơi lại thành công");
});
wsService.on('REMATCH_ERROR', function(data) {
    alert("Lỗi: " + data.message);
    // Có thể tự động về lobby
    setTimeout(() => {
        window.goToLobby();
    }, 2000);
});
function updateUserStats(result) {
    // Lấy thống kê hiện tại
    let stats = JSON.parse(localStorage.getItem('userStats')) || {
        wins: 0,
        losses: 0,
        draws: 0
    };
    
    if (result === "win") {
        stats.wins++;
    } else if (result === "lose") {
        stats.losses++;
    } else if (result === "draw") {
        stats.draws++;
    }
    
    // Lưu lại
    localStorage.setItem('userStats', JSON.stringify(stats));
    
    // Có thể gửi lên server để lưu vào DB
    // updateStatsToServer(stats);
}
   function updateStatsOnScreen() {
    const user = JSON.parse(localStorage.getItem('user'));
    if (!user) return;
    
    // Lấy stats từ localStorage hoặc server
    const stats = JSON.parse(localStorage.getItem('userStats')) || {
        wins: 0,
        losses: 0,
        draws: 0,
        score: 1000
    };
    
    // Cập nhật lên giao diện
    document.getElementById('player1Wins').textContent = stats.wins;
    document.getElementById('player1WinRate').textContent = 
        stats.wins + stats.losses + stats.draws > 0 
        ? Math.round((stats.wins / (stats.wins + stats.losses + stats.draws)) * 100) + '%'
        : '0%';
    document.getElementById('player1Score').textContent = stats.score;
    
    // Cập nhật số trận hòa (nếu có element)
    const drawsElement = document.getElementById('player1Draws');
    if (drawsElement) {
        drawsElement.textContent = stats.draws;
    }
}
    
    // Nút Đầu Hàng
   document.getElementById('btnSurrender').addEventListener('click', function() {
    Swal.fire({
        title: 'Đầu hàng?',
        text: "Bạn sẽ bị tính thua nếu đầu hàng!",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Đầu hàng',
        cancelButtonText: 'Tiếp tục'
    }).then((result) => {
        if (result.isConfirmed) {
            wsService.send({
                type: 'SURRENDER',
                roomId: roomInfo.roomId,
                username: user.username
            });
        }
    });
});

wsService.on('UNDO_SUCCESS', () => {
    alert("Đã chấp nhận đi lại!");
    // Logic xóa quân cờ cuối cùng trên bàn cờ HTML
    // Bạn cần viết hàm removeLastMove() trong JS: tìm ô vừa đánh xóa textContent và class đi
    // Đồng thời xóa dòng log trong bảng "Nước đi gần đây"
});
    // Chat
    document.getElementById('btnSend').addEventListener('click', sendMessage);
    
    function sendMessage() {
    const input = document.getElementById("chatInput");
    const text = input.value.trim();

    if (text) {
      console.log("Gửi tin nhắn chat:", {
        roomId: roomInfo.roomId,
        sender: user.username,
        text: text,
      });

      // Gửi tin nhắn lên server
      wsService.send({
        type: "CHAT",
        roomId: roomInfo.roomId,
        sender: user.username,
        text: text,
      });

      // Xóa input
      input.value = "";

      // KHÔNG append tin nhắn ở đây - chờ server gửi lại
    }
  }

  // 4.2 Lắng nghe tin nhắn từ server
  wsService.on("CHAT", function (data) {
    console.log("Nhận tin nhắn chat từ server:", data);

    // Kiểm tra xem tin nhắn có thuộc phòng hiện tại không
    if (data.roomId === roomInfo.roomId) {
      const isOwnMessage = data.sender === user.username;
      appendMessage(data.sender, data.text, isOwnMessage ? "own" : "opponent");
    } else {
      console.log("Tin nhắn không thuộc phòng hiện tại:", data.roomId);
    }
  });

  // 4.3 Hàm hiển thị tin nhắn
  function appendMessage(sender, text, type) {
    const list = document.getElementById("chatMessages");
    const msg = document.createElement("div");
    msg.className = `message ${type}`;
    msg.innerHTML = `<div class="message-sender">${sender}</div><div class="message-text">${escapeHtml(
      text
    )}</div>`;
    list.appendChild(msg);

    // Tự động cuộn xuống tin nhắn mới nhất
    list.scrollTop = list.scrollHeight;

    // Hiệu ứng cho tin nhắn mới
    msg.style.animation = "fadeIn 0.3s ease-in";
  }

  // 4.4 Escape HTML để tránh XSS
  function escapeHtml(text) {
    const div = document.createElement("div");
    div.textContent = text;
    return div.innerHTML;
  }

  // 4.5 Xử lý sự kiện nhấn Enter để gửi tin nhắn
  document
    .getElementById("chatInput")
    .addEventListener("keypress", function (e) {
      if (e.key === "Enter") {
        e.preventDefault();
        sendMessage();
      }
    });

  // 4.6 Gắn sự kiện cho nút gửi
  document.getElementById("btnSend").addEventListener("click", sendMessage);
    function appendSystemMessage(text) {
    const list = document.getElementById("chatMessages");
    const msg = document.createElement("div");
    msg.className = "message message-system";
    msg.textContent = text;
    list.appendChild(msg);
    list.scrollTop = list.scrollHeight;
  }
    // --- XỬ LÝ KHI ĐỐI THỦ THOÁT ---
  
    // --- SỰ KIỆN NÚT ĐẦU HÀNG ---
    document.getElementById('btnSurrender').addEventListener('click', function() {
        if(confirm("Bạn chắc chắn muốn đầu hàng không?")) {
            wsService.send({
                type: 'SURRENDER',
                roomId: roomInfo.roomId,
                username: user.username
            });
        }
    });

    // --- SỰ KIỆN NÚT VỀ PHÒNG CHỜ (Trong bảng kết quả) ---
    
    window.goToLobby = function() {
    
    wsService.send({
        type: 'LEAVE_ROOM',
        roomId: roomInfo.roomId,
        username: user.username
    });
    
    // Xóa data trận đấu cũ
    localStorage.removeItem('currentRoom');
    
    // Về phòng chờ
    window.location.href = 'lobby.html';
}
    

    window.playAgain = function() {
    
    if (roomInfo.opponent === "BOT_SIEU_CAP") {
        wsService.send({
            type: 'PLAY_WITH_BOT',
            username: user.username
        });
    } else {
        
        sendRematchRequest();
    }
}
const btnLeave = document.getElementById('btnLeaveGame');
    if (btnLeave) {
        btnLeave.onclick = function() {
            Swal.fire({
                title: 'Rời phòng?',
                text: "Nếu rời phòng lúc này bạn sẽ bị xử THUA. Bạn chắc chắn chứ?",
                icon: 'warning',
                showCancelButton: true,
                confirmButtonColor: '#d33',
                cancelButtonColor: '#3085d6',
                confirmButtonText: 'Rời đi',
                cancelButtonText: 'Ở lại'
            }).then((result) => {
                if (result.isConfirmed) {
                    // 1. Cố gắng gửi lệnh lên Server (Dùng try-catch để tránh lỗi làm kẹt trang)
                    try {
                        if (wsService && wsService.send) {
                            wsService.send({
                                type: 'LEAVE_ROOM',
                                roomId: roomInfo.roomId,
                                username: user.username
                            });
                        }
                    } catch (e) {
                        console.log("Lỗi gửi socket (không quan trọng):", e);
                    }

                    // 2. Xử lý phía người rời đi: Xóa data và về sảnh ngay lập tức
                    localStorage.removeItem('currentRoom');
                    window.location.href = 'lobby.html';
                }
            });
        };
    }
});


function initializeBoard() {
    const board = document.getElementById('gameBoard');
    board.innerHTML = '';
    for (let row = 0; row < 15; row++) {
        for (let col = 0; col < 15; col++) {
            const cell = document.createElement('div');
            cell.className = 'board-cell';
            
            const content = document.createElement('div');
            content.className = 'cell-content';
            content.id = `cell-${row}-${col}`;
            cell.appendChild(content);
            
            // Gán sự kiện click
            cell.addEventListener('click', () => window.handleCellClick(row, col));
            
            board.appendChild(cell);
        }
    }
}

function updateMovesList(data) {
    const movesList = document.getElementById('player1Moves');
    
  
    const emptyMsg = movesList.querySelector('.text-muted');
    if(emptyMsg) emptyMsg.remove();

    const moveItem = document.createElement('div');
    moveItem.style.cssText = `padding: 5px; margin-bottom: 5px; background: rgba(255,255,255,0.1); border-radius: 4px; border-left: 3px solid ${data.symbol === 'X' ? '#00d4ff' : '#ff0080'};`;
    
    const colLetter = String.fromCharCode(65 + parseInt(data.col));
    const time = new Date().toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
    
    moveItem.innerHTML = `<div style="display:flex; justify-content:space-between; color:white"><span>${data.symbol}</span> <small style="color:#aaa">${time}</small></div><div style="color:white">${colLetter}${parseInt(data.row) + 1}</div>`;
    
    movesList.prepend(moveItem);
}


   
    window.addEventListener('beforeunload', function (e) {
    
    e.returnValue = 'Nếu thoát, bạn sẽ bị tính thua. Bạn chắc chắn?';
    
    
    wsService.send({
        type: 'SURRENDER',
        roomId: roomInfo.roomId,
        username: user.username
    });
    const btnLeave = document.getElementById('btnLeaveGame');
    if (btnLeave) {
        btnLeave.onclick = function() {
            Swal.fire({
                title: 'Rời phòng?',
                text: "Nếu rời phòng lúc này bạn sẽ bị xử THUA. Bạn chắc chắn chứ?",
                icon: 'warning',
                showCancelButton: true,
                confirmButtonColor: '#d33',
                cancelButtonColor: '#3085d6',
                confirmButtonText: 'Rời đi',
                cancelButtonText: 'Ở lại'
            }).then((result) => {
                if (result.isConfirmed) {
                    // 1. Cố gắng gửi lệnh lên Server (Dùng try-catch để tránh lỗi làm kẹt trang)
                    try {
                        if (wsService && wsService.send) {
                            wsService.send({
                                type: 'LEAVE_ROOM',
                                roomId: roomInfo.roomId,
                                username: user.username
                            });
                        }
                    } catch (e) {
                        console.log("Lỗi gửi socket (không quan trọng):", e);
                    }

                    // 2. Xử lý phía người rời đi: Xóa data và về sảnh ngay lập tức
                    localStorage.removeItem('currentRoom');
                    window.location.href = 'lobby.html';
                }
            });
        };
    }
});

 
    function leaveGame() {
        
        wsService.send({
            type: 'LEAVE_ROOM',
            roomId: roomInfo.roomId,
            username: user.username
        });
        
      
        localStorage.removeItem('currentRoom');
        
       
        window.location.href = 'lobby.html';
    }

    //
wsService.on('OPPONENT_LEFT', () => {
        // 1. Cập nhật nội dung bảng chiến thắng
        document.getElementById('victoryTitle').textContent = "CHIẾN THẮNG!";
        document.getElementById('victoryMessage').textContent = "Đối thủ đã thoát trận / mất kết nối.";
       
        document.getElementById('victoryOverlay').style.display = 'flex';
        
        
        localStorage.removeItem('currentRoom');
        
   
    });

//
document.getElementById('btnLeaveGame').addEventListener('click', function() {
    if(confirm("Rời phòng sẽ bị tính thua. Bạn chắc chắn?")) {
        
        wsService.send({
            type: 'SURRENDER',
            roomId: roomInfo.roomId,
            username: user.username
        });
        
       
        showVictoryScreen("THẤT BẠI", 
            "Bạn đã rời phòng. Đối thủ được tính thắng!", 
            "lose");
        
        
        setTimeout(() => {
            window.location.href = 'lobby.html';
        }, 3000);
    }
});
function resetGameUI(newRoomInfo) {
  
  
    window.gameOver = false; 

  
    const cells = document.querySelectorAll('.cell-content');
    cells.forEach(cell => {
        cell.textContent = '';
        cell.className = 'cell-content'; 
        cell.style.animation = '';
    });

   
    document.getElementById('player1Moves').innerHTML = 
        '<div style="color: var(--text-muted); text-align: center; padding: 20px;">Ván mới bắt đầu</div>';

 
    document.getElementById('victoryOverlay').style.display = 'none';

    
    document.getElementById('currentPlayer').textContent = 
        (newRoomInfo.yourSymbol === 'X') ? user.username : newRoomInfo.opponent;

    
    startClientTimer(60);
}

function updateTurnInfo(playerName, symbol) {
    const turnText = document.getElementById('turnIndicator');
    const playerSpan = document.getElementById('currentPlayer');

    
    playerSpan.textContent = playerName;

    if (symbol === 'X') {
        turnText.style.color = '#00d4ff'; 
        turnText.style.textShadow = '0 0 10px #00d4ff';
        turnText.style.borderColor = '#00d4ff';
    } else {
        turnText.style.color = '#ff0055'; 
        turnText.style.textShadow = '0 0 10px #ff0055';
        turnText.style.borderColor = '#ff0055';
    }

    const isPlayer1Turn = (document.getElementById('player1Name').textContent === playerName);
    
    if (isPlayer1Turn) {
        document.querySelector('.player-1').classList.add('active-turn');
        document.querySelector('.player-2').classList.remove('active-turn');
    } else {
        document.querySelector('.player-1').classList.remove('active-turn');
        document.querySelector('.player-2').classList.add('active-turn');
    }
}