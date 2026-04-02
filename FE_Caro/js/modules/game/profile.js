// Lấy thông tin user từ localStorage
const currentUser = JSON.parse(localStorage.getItem("user"));

// Lắng nghe sự kiện AVATAR_CHANGED từ WebSocket
wsService.on("AVATAR_CHANGED", function (data) {
  console.log("Nhận AVATAR_CHANGED từ server:", data);

  // Cập nhật avatar trong localStorage
  let user = JSON.parse(localStorage.getItem("user"));
  if (user) {
    user.avatar = data.avatar;
    localStorage.setItem("user", JSON.stringify(user));
  }

  // Cập nhật giao diện
  const profileAvatar = document.getElementById("profileAvatar");
  if (profileAvatar) {
    profileAvatar.src = "images/avatars/" + data.avatar;
  }

  // Cập nhật preview trong modal nếu đang mở
  const currentAvatarPreview = document.getElementById("currentAvatarPreview");
  if (currentAvatarPreview) {
    currentAvatarPreview.src = "images/avatars/" + data.avatar;
  }

  // Hiển thị thông báo thành công
  const avatarStatus = document.getElementById("avatarStatus");
  if (avatarStatus) {
    avatarStatus.style.display = "block";
    setTimeout(() => {
      avatarStatus.style.display = "none";
    }, 3000);
  }

  // Reset nút Save
  const saveAvatarBtn = document.getElementById("saveAvatarBtn");
  if (saveAvatarBtn) {
    saveAvatarBtn.innerHTML = '<i class="fas fa-save"></i> Lưu Avatar';
    saveAvatarBtn.disabled = false;
  }
});

// Hàm gửi yêu cầu đổi avatar
function sendChangeAvatarRequest(avatar) {
  if (currentUser && wsService) {
    wsService.send({
      type: "CHANGE_AVATAR",
      username: currentUser.username,
      avatar: avatar,
    });
  }
}

// Export function để profile.html có thể sử dụng
window.sendChangeAvatarRequest = sendChangeAvatarRequest;

// --- Elo history / chart handling ---
function requestEloHistory(limit = 20) {
  if (currentUser && window.wsService && wsService.send) {
    wsService.send({
      type: "GET_ELO_HISTORY",
      username: currentUser.username,
      limit: limit,
    });
  } else {
    const stored = JSON.parse(localStorage.getItem("eloHistory")) || [];
    if (window.initializeChart) window.initializeChart(stored);
  }
}

// Handle server response for elo history
if (window.wsService && wsService.on) {
  wsService.on("ELO_HISTORY", function (data) {
    console.log("Received ELO_HISTORY", data);
    // Accept multiple possible payload shapes
    let history = [];
    if (Array.isArray(data.history)) history = data.history;
    else if (Array.isArray(data.eloHistory)) history = data.eloHistory;
    else if (Array.isArray(data.elo))
      history = data.elo.map((v) => ({ elo: v }));

    // Normalize: ensure each item has .elo
    history = history.map((item) => {
      if (typeof item === "number") return { elo: item };
      if (item && item.elo !== undefined) return item;
      // try common keys
      if (item && item.value !== undefined) return { elo: item.value };
      return { elo: 1500 };
    });

    // Save fallback
    localStorage.setItem("eloHistory", JSON.stringify(history));

    if (window.initializeChart) window.initializeChart(history);
  });
}

// Request on load
try {
  if (currentUser) requestEloHistory(20);
} catch (e) {
  console.error("Error requesting elo history", e);
}

function loadUserInfoFromServer(username) {
    if (!username) return;

    if (window.wsService && wsService.socket && wsService.socket.readyState === WebSocket.OPEN) {
        console.log("Gửi yêu cầu lấy thông tin user:", username);
        wsService.send({
            type: "GET_USER_INFO",
            username: username
        });
    } else {
        console.warn("Socket chưa sẵn sàng, thử lại sau 500ms...");
        setTimeout(() => loadUserInfoFromServer(username), 500);
    }
}

function updateProfileWithLocalData(user) {
    
    document.getElementById('profileUsername').textContent = user.username;
    document.getElementById('infoUsername').textContent = user.username;
    document.getElementById('infoEmail').textContent = user.email || "Chưa cập nhật";
    
    // Elo và stats
    document.getElementById('userElo').textContent = `Elo: ${user.elo || 1500}`;
    document.getElementById('statsElo').textContent = user.elo || 1500;
    
    // Tính tỷ lệ thắng từ wins/losses nếu có
    const totalGames = (user.wins || 0) + (user.losses || 0);
    const winRate = totalGames > 0 ? ((user.wins || 0) / totalGames * 100).toFixed(1) : 0;
    
    document.getElementById('statsWins').textContent = user.wins || 0;
    document.getElementById('statsLosses').textContent = user.losses || 0;
    document.getElementById('statsWinRate').textContent = `${winRate}%`;
    document.getElementById('statsTotalGames').textContent = totalGames;
    document.getElementById('totalGames').textContent = totalGames;
    
    // Ngày tham gia (mặc định)
    document.getElementById('joinDate').textContent = new Date().toLocaleDateString('vi-VN');
    document.getElementById('highestElo').textContent = user.elo || 1500;
}
function updateProfileWithLocalData(user) {
    // Cập nhật thông tin cơ bản
    document.getElementById('profileUsername').textContent = user.username;
    document.getElementById('infoUsername').textContent = user.username;
    document.getElementById('infoEmail').textContent = user.email || "Chưa cập nhật";
    
    // Elo và stats
    document.getElementById('userElo').textContent = `Elo: ${user.elo || 1500}`;
    document.getElementById('statsElo').textContent = user.elo || 1500;
    
    // Tính tỷ lệ thắng từ wins/losses nếu có
    const totalGames = (user.wins || 0) + (user.losses || 0);
    const winRate = totalGames > 0 ? ((user.wins || 0) / totalGames * 100).toFixed(1) : 0;
    
    document.getElementById('statsWins').textContent = user.wins || 0;
    document.getElementById('statsLosses').textContent = user.losses || 0;
    document.getElementById('statsWinRate').textContent = `${winRate}%`;
    document.getElementById('statsTotalGames').textContent = totalGames;
    document.getElementById('totalGames').textContent = totalGames;
    
    // Ngày tham gia (mặc định)
    document.getElementById('joinDate').textContent = new Date().toLocaleDateString('vi-VN');
    document.getElementById('highestElo').textContent = user.elo || 1500;
}