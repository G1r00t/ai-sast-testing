from flask import Flask, request, session, jsonify
import pickle
import base64
import yaml
import hashlib
import os
from datetime import datetime

app = Flask(__name__)
app.secret_key = os.urandom(16)

# Simulated user database
users_db = {
    "admin": {"password_hash": hashlib.md5(b"admin123").hexdigest(), "role": "admin"},
    "user": {"password_hash": hashlib.md5(b"user123").hexdigest(), "role": "user"}
}

# VULNERABILITY 1: Insecure Deserialization via Pickle
# The pickle module can execute arbitrary code during deserialization
@app.route('/api/load_profile', methods=['POST'])
def load_profile():
    """
    Load user profile from base64-encoded pickle data.
    Vulnerable to arbitrary code execution via pickle deserialization.
    """
    try:
        profile_data = request.json.get('profile_data')
        if profile_data:
            # VULN: Unsafe deserialization of user-controlled data
            decoded = base64.b64decode(profile_data)
            profile = pickle.loads(decoded)  # Arbitrary code execution possible
            return jsonify({"status": "success", "profile": str(profile)})
        return jsonify({"error": "No profile data provided"}), 400
    except Exception as e:
        return jsonify({"error": str(e)}), 500


# VULNERABILITY 2: YAML Deserialization with unsafe loader
# PyYAML's unsafe loader can instantiate arbitrary Python objects
@app.route('/api/import_config', methods=['POST'])
def import_config():
    """
    Import configuration from YAML.
    Vulnerable to arbitrary code execution via YAML deserialization.
    """
    try:
        yaml_data = request.json.get('config')
        if yaml_data:
            # VULN: Using unsafe YAML loader that can execute arbitrary Python code
            config = yaml.load(yaml_data, Loader=yaml.Loader)  # Unsafe loader
            return jsonify({"status": "success", "config": config})
        return jsonify({"error": "No config provided"}), 400
    except Exception as e:
        return jsonify({"error": str(e)}), 500


# VULNERABILITY 3: Timing Attack on Authentication
# Using non-constant-time comparison for password verification
@app.route('/api/login', methods=['POST'])
def login():
    """
    User login endpoint.
    Vulnerable to timing attacks due to early-exit string comparison.
    """
    username = request.json.get('username')
    password = request.json.get('password')
    
    if username in users_db:
        user = users_db[username]
        password_hash = hashlib.md5(password.encode()).hexdigest()
        
        # VULN: Non-constant time comparison allows timing attacks
        # Attacker can deduce password character-by-character by measuring response time
        if password_hash == user['password_hash']:  # Early exit on mismatch
            session['username'] = username
            session['role'] = user['role']
            return jsonify({"status": "success", "message": "Login successful"})
    
    return jsonify({"status": "error", "message": "Invalid credentials"}), 401


# VULNERABILITY 4: Weak Cryptographic Hash (MD5) for Password Storage
# MD5 is cryptographically broken and unsuitable for password hashing
@app.route('/api/register', methods=['POST'])
def register():
    """
    User registration endpoint.
    Vulnerable due to weak MD5 hashing without salt.
    """
    username = request.json.get('username')
    password = request.json.get('password')
    
    if username in users_db:
        return jsonify({"error": "User already exists"}), 400
    
    # VULN: Using MD5 (broken hash) without salt for password storage
    # MD5 is vulnerable to rainbow table attacks and collision attacks
    password_hash = hashlib.md5(password.encode()).hexdigest()
    
    users_db[username] = {
        "password_hash": password_hash,
        "role": "user"
    }
    
    return jsonify({"status": "success", "message": "User registered"})


@app.route('/api/status', methods=['GET'])
def status():
    """Simple status endpoint to verify the app is running."""
    return jsonify({
        "status": "running",
        "timestamp": datetime.now().isoformat(),
        "endpoints": [
            "/api/load_profile (POST)",
            "/api/import_config (POST)",
            "/api/login (POST)",
            "/api/register (POST)",
            "/api/status (GET)"
        ]
    })


if __name__ == '__main__':
    print("Starting vulnerable Flask application for SAST testing...")
    print("Available endpoints:")
    print("  - POST /api/load_profile")
    print("  - POST /api/import_config")
    print("  - POST /api/login")
    print("  - POST /api/register")
    print("  - GET  /api/status")
    app.run(debug=True, host='0.0.0.0', port=5000)
