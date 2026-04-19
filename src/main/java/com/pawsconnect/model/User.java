package com.pawsconnect.model;

    public class User {
        private int id;
        private String fullName;
        private String email;
        private String passwordHash;
        private String role;

        public User() {}

        public User(int id, String fullName, String email, String passwordHash, String role) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
            this.passwordHash = passwordHash;
            this.role = role;
        }

        // Keep old constructor for backward compatibility
        public User(int id, String fullName, String email, String passwordHash) {
            this(id, fullName, email, passwordHash, "user");
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPasswordHash() { return passwordHash; }
        public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        // Convenience — use this everywhere instead of string comparison
        public boolean isAdmin() {
            return "admin".equalsIgnoreCase(this.role);
        }
    }

