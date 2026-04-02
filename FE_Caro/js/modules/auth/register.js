document.addEventListener("DOMContentLoaded", function() {
    const registerForm = document.getElementById('registerForm');
    const loading = document.getElementById('loading');

    
    function setupTogglePassword(btnId, inputId) {
        const btn = document.getElementById(btnId);
        const input = document.getElementById(inputId);
        if(btn && input) {
            btn.addEventListener('click', function() {
                const type = input.getAttribute('type') === 'password' ? 'text' : 'password';
                input.setAttribute('type', type);
                this.querySelector('i').classList.toggle('fa-eye');
                this.querySelector('i').classList.toggle('fa-eye-slash');
            });
        }
    }
    setupTogglePassword('toggleRegPassword', 'regPassword');
    setupTogglePassword('toggleConfirmPassword', 'regConfirmPassword');

    
    registerForm.addEventListener('submit', function(e) {
        e.preventDefault();

        // Xóa thông báo lỗi cũ
        document.querySelectorAll('.error-message').forEach(el => el.textContent = '');

        const username = document.getElementById('regUsername').value;
        const email = document.getElementById('regEmail').value;
        const password = document.getElementById('regPassword').value;
        const confirmPass = document.getElementById('regConfirmPassword').value;
        const acceptTerms = document.getElementById('acceptTerms').checked;

        // --- ĐOẠN CODE MỚI THÊM Ở ĐÂY ---
        // Kiểm tra độ dài mật khẩu
        if (password.length < 8) {
            document.getElementById('passwordError').textContent = "Mật khẩu phải có ít nhất 8 ký tự!";
            return; // Dừng lại, không gửi lên server
        }
        // --------------------------------

        // Kiểm tra mật khẩu xác nhận
        if (password !== confirmPass) {
            document.getElementById('confirmPasswordError').textContent = "Mật khẩu xác nhận không khớp!";
            return;
        }
        
        if (!acceptTerms) {
            document.getElementById('termsError').textContent = "Bạn phải đồng ý với điều khoản!";
            return;
        }

        loading.style.display = 'flex';

        wsService.send({
            type: 'REGISTER',
            username: username,
            password: password,
            email: email
        });
    });

    
    wsService.on('REGISTER_SUCCESS', (data) => {
        loading.style.display = 'none';
        alert("Đăng ký thành công! Hãy đăng nhập ngay.");
        window.location.href = 'login.html';
    });

    wsService.on('REGISTER_FAIL', (data) => {
        loading.style.display = 'none';
        
        document.getElementById('usernameError').textContent = data.message;
    });
});