document.addEventListener('DOMContentLoaded', async () => {

 // 1. Check session on every page load 
 try {
 const res = await fetch('api/session');
 if (res.ok) {
 const data = await res.json();
 window.currentUser = data.loggedIn ? data.user : null;
 updateNavbar(data);
 } else {
 updateNavbar({ loggedIn: false });
 }
 } catch (err) {
 console.error('Session check failed:', err);
 updateNavbar({ loggedIn: false });
 }

 // 2. Email form intercept (login.html only) 
 const emailPanel = document.getElementById('emailPanel');
 if (emailPanel) {
 emailPanel.addEventListener('submit', async (e) => {
 e.preventDefault();

 if (typeof checkRateLimit === 'function' && !checkRateLimit()) return;

 const isRegister = emailPanel.getAttribute('action') === 'register';
 const actionUrl = isRegister
 ? 'register'
 : 'login';

 // Client-side validation 
 const email = document.getElementById('emailInput').value.trim();
 const password = document.getElementById('passwordInput').value;
 let valid = true;

 if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
 if (typeof showError === 'function') showError('emailError', 'Enter a valid email address.');
 valid = false;
 }
 if (password.length < 6) {
 if (typeof showError === 'function') showError('passError', 'Password must be at least 6 characters.');
 valid = false;
 }
 if (isRegister) {
 const name = (document.getElementById('fullName')?.value || '').trim();
 const confirm = document.getElementById('confirmInput')?.value || '';
 if (!name) {
 if (typeof showError === 'function') showError('nameError', 'Please enter your full name.');
 valid = false;
 }
 if (password !== confirm) {
 if (typeof showError === 'function') showError('confirmError', 'Passwords do not match.');
 valid = false;
 }
 }
 if (!valid) {
 if (typeof recordFailure === 'function') recordFailure();
 return;
 }

 // Show spinner 
 if (typeof setLoading === 'function') {
 setLoading('emailSubmitBtn', 'emailBtnText', 'emailSpinner', true);
 }

 try {
 const formData = new FormData(emailPanel);
 const response = await fetch(actionUrl, {
 method: 'POST',
 body: new URLSearchParams(formData)
 });

 const result = await response.json();

 if (response.ok && result.success) {
 if (isRegister) {
 // Registration success switch to sign in tab
 alert('Account created! Please sign in.');
 if (typeof switchMode === 'function') switchMode('login');
 } else {
 // Login success admin goes to panel, user goes to index
 if (result.role === 'admin') {
 window.location.href = 'admin.html';
 } else {
 const returnTo = new URLSearchParams(window.location.search).get('return') || 'index.html';
 window.location.href = returnTo;
 }
 }
 } else {
 const msg = result.error || 'Authentication failed.';
 if (typeof showError === 'function') {
 showError('emailError', msg);
 } else {
 alert(msg);
 }
 if (typeof recordFailure === 'function') recordFailure();
 }

 } catch (err) {
 console.error('Auth error:', err);
 if (typeof showError === 'function') {
 showError('emailError', 'Network error is Tomcat running?');
 }
 } finally {
 if (typeof setLoading === 'function') {
 setLoading('emailSubmitBtn', 'emailBtnText', 'emailSpinner', false);
 }
 }
 });
 }
});

// Navbar injection 
function updateNavbar(sessionData) {
 const navLinks = document.getElementById('navLinks');
 if (!navLinks) return;

 // Clear previously injected auth controls to avoid duplicates on re-render.
 navLinks.querySelectorAll('[data-auth-dynamic="1"]').forEach(el => el.remove());

 if (sessionData.loggedIn) {
 const user = sessionData.user;
 const firstName = ((user && user.fullName) ? user.fullName : 'User').split(' ')[0];
 const isAdmin = !!(user && user.role === 'admin');

 const greetingLi = document.createElement('li');
 greetingLi.setAttribute('data-auth-dynamic', '1');
 greetingLi.innerHTML =
 `<span style="color:var(--forest);font-weight:500;">Hi, ${firstName}${isAdmin ? ' ' : ''}</span>`;
 navLinks.appendChild(greetingLi);

 if (isAdmin) {
 const adminLi = document.createElement('li');
 adminLi.setAttribute('data-auth-dynamic', '1');
 const adminBtn = document.createElement('a');
 adminBtn.href = 'admin.html';
 adminBtn.className = 'nav-cta';
 adminBtn.style.cssText = 'background:#8b5e3c;';
 adminBtn.textContent = 'Admin';
 adminBtn.title = 'Admin Panel';
 adminLi.appendChild(adminBtn);
 navLinks.appendChild(adminLi);
 }

 const logoutLi = document.createElement('li');
 logoutLi.setAttribute('data-auth-dynamic', '1');
 const logoutBtn = document.createElement('a');
 logoutBtn.href = '#';
 logoutBtn.className = 'nav-cta';
 logoutBtn.style.cssText = 'background:var(--moss);';
 logoutBtn.textContent = 'Logout';
 logoutBtn.addEventListener('click', async (e) => {
 e.preventDefault();
 await fetch('logout');
 window.location.reload();
 });
 logoutLi.appendChild(logoutBtn);
 navLinks.appendChild(logoutLi);

 } else {
 const joinLi = document.createElement('li');
 joinLi.setAttribute('data-auth-dynamic', '1');
 const joinBtn = document.createElement('a');
 joinBtn.href = 'login.html';
 joinBtn.className = 'nav-cta';
 joinBtn.textContent = 'Join Free';
 joinLi.appendChild(joinBtn);
 navLinks.appendChild(joinLi);
 }
}