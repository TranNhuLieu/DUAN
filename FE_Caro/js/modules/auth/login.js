document.addEventListener("DOMContentLoaded", function () {
  const loginForm = document.getElementById("loginForm");
  const loading = document.getElementById("loading");

  const toggleBtn = document.getElementById("togglePassword");
  const passInput = document.getElementById("password");

  if (toggleBtn) {
    toggleBtn.addEventListener("click", function () {
      const type =
        passInput.getAttribute("type") === "password" ? "text" : "password";
      passInput.setAttribute("type", type);

      this.querySelector("i").classList.toggle("fa-eye");
      this.querySelector("i").classList.toggle("fa-eye-slash");
    });
  }

  loginForm.addEventListener("submit", function (e) {
    e.preventDefault();

    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;

    loading.style.display = "flex";

    wsService.send({
      type: "LOGIN",
      username: username,
      password: password,
    });
  });

  wsService.on("LOGIN_SUCCESS", (data) => {
    loading.style.display = "none";
    console.log("LOGIN_SUCCESS data:", data); // Debug log
    alert("Đăng nhập thành công!");

    // Lưu thông tin user kèm avatar vào localStorage
    localStorage.setItem(
      "user",
      JSON.stringify({
        username: data.username,
        avatar: data.avatar || "1.jpg",
        email: data.email,
        elo: data.elo || 1000,
        wins: data.wins || 0,
        losses: data.losses || 0,
        winRate: data.winRate || 0
      })
    );

    window.location.href = "lobby.html";
  });

  wsService.on("LOGIN_FAIL", (data) => {
    loading.style.display = "none";

    document.getElementById("passwordError").textContent = data.message;
    document.getElementById("passwordError").style.display = "block";
  });
});
