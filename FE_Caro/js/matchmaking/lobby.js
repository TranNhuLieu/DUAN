// js/modules/matchmaking/lobby.js
console.log('Lobby JS loaded');

document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM fully loaded');
    
    // Kiểm tra đăng nhập
    const user = localStorage.getItem('user');
    if (!user) {
        alert('Bạn chưa đăng nhập!');
        window.location.href = 'login.html';
        return;
    }
    
    const userData = JSON.parse(user);
    console.log('User data:', userData);
    
    // Cập nhật tên người dùng
    const usernameDisplay = document.getElementById('usernameDisplay');
    if (usernameDisplay) {
        usernameDisplay.textContent = userData.username;
    }
    
    // Cập nhật avatar
    const userAvatar = document.getElementById('userAvatar');
    if (userAvatar) {
        const avatarPath = `images/avatars/${userData.avatar || '1.jpg'}`;
        userAvatar.src = avatarPath;
        userAvatar.onerror = function() {
            this.src = 'images/avatars/1.jpg';
        };
        console.log('Avatar set to:', avatarPath);
    }
    
    // Cập nhật stats display
    if (userData.elo) {
        const eloDisplay = document.getElementById('eloDisplay');
        if (eloDisplay) {
            eloDisplay.textContent = `Elo: ${userData.elo}`;
        }
    }
    
    const statElo = document.getElementById('statElo');
    const statWins = document.getElementById('statWins');
    const statLosses = document.getElementById('statLosses');
    const statRate = document.getElementById('statRate');
    
    if (statElo) statElo.textContent = userData.elo || 1500;
    if (statWins) statWins.textContent = userData.wins || 0;
    if (statLosses) statLosses.textContent = userData.losses || 0;
    if (statRate) {
        const winRate = userData.wins || 0;
        const totalGames = (userData.wins || 0) + (userData.losses || 0);
        const rate = totalGames > 0 ? (winRate / totalGames * 100).toFixed(1) : 0;
        statRate.textContent = `${rate}%`;
    }
    
    // Thêm click handler cho avatar để vào profile (chỉ khi click vào avatar, không phải button camera)
    if (userAvatar) {
        userAvatar.style.cursor = 'pointer';
        userAvatar.addEventListener('click', function(e) {
            // Kiểm tra xem có click vào button camera không
            if (e.target.classList.contains('avatar-change-btn') || 
                e.target.closest('.avatar-change-btn')) {
                return; // Không làm gì nếu click vào button camera
            }
            console.log('Avatar clicked - going to profile');
            window.location.href = 'profile.html';
        });
    }
    
    const quickPlayBtn = document.getElementById('quickPlayBtn');
    const playBotBtn = document.getElementById('playBotBtn');
    const cancelSearchBtn = document.getElementById('cancelSearchBtn');
    const matchStatus = document.getElementById('matchStatus');
    const searchTimer = document.getElementById('searchTimer');
    
    console.log('Quick play button:', quickPlayBtn);
    console.log('Cancel button:', cancelSearchBtn);
    console.log('Match status:', matchStatus);
    
    // Biến trạng thái
    let isSearching = false;
    let searchSeconds = 0;
    let searchInterval = null;
    const MAX_SEARCH_TIME = 30; 
    
    // Sự kiện click nút "Tìm trận"
    if (quickPlayBtn) {
        quickPlayBtn.addEventListener('click', function() {
            console.log('Tìm trận button clicked!');
            if (!isSearching) {
                startMatchmaking();
            }
        });
    }
    if (playBotBtn) {
        playBotBtn.addEventListener('click', function() {
            console.log('Bấm nút đấu với máy!');
            
            // Lấy thông tin user từ localStorage
            const user = JSON.parse(localStorage.getItem('user'));
            if (!user) return alert("Vui lòng đăng nhập lại!");

            // Gửi yêu cầu lên Server
            if (window.wsService && wsService.socket.readyState === WebSocket.OPEN) {
                wsService.send({
                    type: 'PLAY_WITH_BOT',
                    username: user.username
                });
                
                // Hiển thị thông báo chờ chút xíu
                playBotBtn.disabled = true;
                playBotBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang tạo phòng...';
            } else {
                alert("Mất kết nối tới Server!");
            }
        });
    }
    // Sự kiện click nút "Hủy"
    if (cancelSearchBtn) {
        cancelSearchBtn.addEventListener('click', function() {
            console.log('Hủy tìm kiếm button clicked!');
            if (isSearching) {
                cancelMatchmaking();
                alert('Đã hủy tìm trận!');
            }
        });
    }
    
    // Bắt đầu tìm trận
   // Thay đổi hàm startMatchmaking
function startMatchmaking() {
    console.log('=== BẮT ĐẦU TÌM TRẬN ===');
    isSearching = true;
    searchSeconds = 0;
    
    // Hiển thị trạng thái
    if (matchStatus) {
        matchStatus.style.display = 'block';
        console.log('Hiển thị trạng thái tìm trận');
    }
    
    if (quickPlayBtn) {
        quickPlayBtn.disabled = true;
        quickPlayBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang tìm...';
    }
    
    // Gửi request lên server
    const user = localStorage.getItem('user');
    if (user) {
        const userData = JSON.parse(user);
        
        const findMatchMessage = {
            type: 'FIND_MATCH',
            username: userData.username
        };
        
        console.log('Gửi message FIND_MATCH:', findMatchMessage);
        
        // Kiểm tra WebSocket
        if (window.wsService) {
            console.log('WebSocket state:', wsService.socket ? wsService.socket.readyState : 'no socket');
            
            if (wsService.socket && wsService.socket.readyState === WebSocket.OPEN) {
                wsService.send(findMatchMessage);
                console.log('Đã gửi FIND_MATCH thành công');
            } else {
                console.error('WebSocket chưa kết nối!');
                alert('Kết nối WebSocket chưa sẵn sàng. Vui lòng đợi...');
                wsService.connect();
                setTimeout(() => {
                    if (wsService.socket.readyState === WebSocket.OPEN) {
                        wsService.send(findMatchMessage);
                    } else {
                        alert('Không thể kết nối. Vui lòng tải lại trang!');
                        cancelMatchmaking();
                    }
                }, 2000);
            }
        } else {
            console.error('wsService không tồn tại!');
            alert('Lỗi WebSocket service!');
        }
    }
    
    // Bắt đầu đếm thời gian
    searchInterval = setInterval(function() {
        searchSeconds++;
        if (searchTimer) {
            searchTimer.textContent = `Đang tìm đối thủ... ${searchSeconds}s`;
        }
        
        // Nếu quá 30 giây thì dừng
        if (searchSeconds >= MAX_SEARCH_TIME) {
            cancelMatchmaking();
            alert('Không tìm thấy đối thủ sau 30 giây. Vui lòng thử lại!');
        }
    }, 1000);
}
    
    // Hủy tìm trận
    function cancelMatchmaking() {
        console.log('Hủy tìm trận...');
        isSearching = false;
        
        // Dừng đếm thời gian
        if (searchInterval) {
            clearInterval(searchInterval);
            searchInterval = null;
        }
        
        // Ẩn trạng thái
        if (matchStatus) matchStatus.style.display = 'none';
        if (quickPlayBtn) quickPlayBtn.disabled = false;
        
        // Gửi request hủy lên server
        if (window.wsService && wsService.send) {
            wsService.send({
                type: 'CANCEL_FIND_MATCH'
            });
        }
    }
    
    // Lắng nghe sự kiện từ server
    if (window.wsService) {
        // Khi tìm thấy trận
        wsService.on('MATCH_FOUND', function(data) {
            console.log('MATCH_FOUND event received:', data);
            cancelMatchmaking();
            
            // Lưu thông tin phòng bao gồm avatar
            localStorage.setItem('currentRoom', JSON.stringify({
                roomId: data.roomId,
                opponent: data.opponent,
                opponentAvatar: data.opponentAvatar || "1.jpg", // Avatar của đối thủ
                yourSymbol: data.yourSymbol
            }));
            
            // Chuyển sang trang game
            alert('Đã tìm thấy đối thủ! Chuyển đến game...');
            window.location.href = 'game.html';
        });
        wsService.on('HISTORY_DATA', function(data) {
            console.log('Nhận data lịch sử:', data);
            // Gọi hàm vẽ bảng bên file HTML
            if (window.renderHistoryTable) {
                window.renderHistoryTable(data.historyList); 
            }
        });

        // 2. Nhận dữ liệu BẢNG XẾP HẠNG từ server gửi về
        wsService.on('RANKING_DATA', function(data) {
            console.log('Nhận data BXH:', data);
            // Gọi hàm vẽ bảng bên file HTML
            if (window.renderRankingTable) {
                window.renderRankingTable(data.rankingList);
            }
        });
        // Khi có lỗi
        wsService.on('ERROR', function(data) {
            console.log('ERROR event received:', data);
            cancelMatchmaking();
            alert('Lỗi: ' + (data.message || 'Không xác định'));
        });
    }
    
    // Sự kiện đăng xuất
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', function() {
            localStorage.removeItem('user');
            window.location.href = 'index.html';
        });
    }

    // Avatar change functionality
    const changeAvatarBtn = document.getElementById('changeAvatarBtn');
    if (changeAvatarBtn) {
        changeAvatarBtn.addEventListener('click', function(e) {
            e.stopPropagation(); // Ngăn event bubble lên avatar
            e.preventDefault();
            console.log('Camera button clicked!');
            openAvatarModal();
        });
    }
});

// Avatar Modal Functions
function openAvatarModal() {
    console.log('Opening avatar modal...');
    const modal = document.getElementById('avatarModal');
    const avatarGrid = document.getElementById('avatarGrid');
    
    console.log('Modal element:', modal);
    console.log('Avatar grid element:', avatarGrid);
    
    if (!modal || !avatarGrid) {
        console.error('Modal hoặc avatar grid không tìm thấy!');
        return;
    }
    
    // Tạo avatar options
    const avatars = ['1.jpg', '2.jpg', '3.jpg', '4.jpg', '5.jpg'];
    const user = JSON.parse(localStorage.getItem('user'));
    const currentAvatar = user.avatar || '1.jpg';
    
    console.log('Current avatar:', currentAvatar);
    
    avatarGrid.innerHTML = '';
    
    avatars.forEach(avatar => {
        const option = document.createElement('div');
        option.className = `avatar-option ${avatar === currentAvatar ? 'selected' : ''}`;
        option.onclick = () => selectAvatar(avatar);
        
        option.innerHTML = `
            <img src="images/avatars/${avatar}" alt="Avatar ${avatar.replace('.jpg', '')}" onerror="this.src='images/avatars/1.jpg'">
            <span>Avatar ${avatar.replace('.jpg', '')}</span>
        `;
        
        avatarGrid.appendChild(option);
    });
    
    console.log('Hiển thị modal...');
    modal.style.display = 'block';
}

function closeAvatarModal() {
    const modal = document.getElementById('avatarModal');
    if (modal) {
        modal.style.display = 'none';
    }
}

function selectAvatar(avatar) {
    // Remove selection from all options
    document.querySelectorAll('.avatar-option').forEach(option => {
        option.classList.remove('selected');
    });
    
    // Select current option
    event.currentTarget.classList.add('selected');
    
    // Send change avatar request
    const user = JSON.parse(localStorage.getItem('user'));
    if (window.wsService && wsService.send && user) {
        wsService.send({
            type: 'CHANGE_AVATAR',
            username: user.username,
            avatar: avatar
        });
    }
}

// Listen for avatar change response
if (window.wsService) {
    wsService.on('AVATAR_CHANGED', function(data) {
        console.log('Avatar changed:', data);
        
        // Update localStorage
        const user = JSON.parse(localStorage.getItem('user'));
        if (user) {
            user.avatar = data.avatar;
            localStorage.setItem('user', JSON.stringify(user));
        }
        
        // Update avatar display
        const userAvatar = document.getElementById('userAvatar');
        if (userAvatar) {
            userAvatar.src = `images/avatars/${data.avatar}`;
        }
        
        // Close modal
        closeAvatarModal();
        
        alert('Đã đổi avatar thành công!');
    });
}

// Close modal when clicking outside
window.onclick = function(event) {
    const modal = document.getElementById('avatarModal');
    if (event.target === modal) {
        closeAvatarModal();
    }
}