const API_BASE = '/api';

// Tự động kiểm tra token khi tải trang (trừ trang login)
(function checkAuth() {
    const token = localStorage.getItem('token');
    const path = window.location.pathname;
    if (!token && !path.endsWith('login.html')) {
        window.location.href = '/login.html';
    }
})();

function getAuthHeaders(extraHeaders = {}) {
    const token = localStorage.getItem('token');
    return {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        ...extraHeaders
    };
}

function logout() {
    localStorage.clear();
    window.location.href = '/login.html';
}

function renderCurrentUser() {
    const username = localStorage.getItem('username') || 'Admin';
    const role = localStorage.getItem('role') || 'STAFF';
    const el = document.getElementById('userDisplay');
    if (el) el.innerText = `${username} (${role})`;

    if (role === 'ADMIN') {
        document.getElementById('staffManagementLink')?.classList.remove('hidden');
    }
}