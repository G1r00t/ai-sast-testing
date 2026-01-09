import os
import subprocess
import sqlite3
import pickle
import base64
import re
import logging
from flask import Flask, request, render_template_string, redirect, url_for
from markupsafe import escape

app = Flask(__name__)

# Set up logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Database setup
def initialize_db():
    conn = sqlite3.connect('users.db')
    c = conn.cursor()
    c.execute('''CREATE TABLE IF NOT EXISTS users
                 (id INTEGER PRIMARY KEY, username TEXT, password TEXT, role TEXT)''')
    conn.commit()
    conn.close()

# --- LIVE VULNERABILITIES (GOOD TO FIX) ---

# Good to fix vulnerability 1: SQL Injection with some partial protection
# REMEDIATED: Using parameterized queries to prevent SQL injection
def authenticate_user(username, password):
    # FIXED: Use parameterized queries instead of string concatenation
    conn = sqlite3.connect('users.db')
    c = conn.cursor()
    # SECURE: Parameterized query prevents SQL injection for both username and password
    query = "SELECT id, username, role FROM users WHERE username = ? AND password = ?"
    c.execute(query, (username, password))
    user = c.fetchone()
    conn.close()
    return user

# Good to fix vulnerability 2: Weak regex for input validation
def validate_email(email):
    # Overly simplistic email validation that could allow some invalid formats
    pattern = r'.+@.+\..+'
    return re.match(pattern, email) is not None

# Good to fix vulnerability 3: Insecure file operations with some path normalization
# REMEDIATED: Using proper path validation and canonicalization
def read_user_file(filename):
    # FIXED: Proper path validation using os.path for secure path handling
    if not filename or not isinstance(filename, str):
        return None
    
    # Normalize and validate the path to prevent directory traversal
    base_dir = os.path.abspath("user_files")
    file_path = os.path.join(base_dir, filename)
    normalized_path = os.path.normpath(file_path)
    
    # Ensure the normalized path is still within the base directory
    if not normalized_path.startswith(base_dir):
        return None
    
    try:
        with open(normalized_path, 'r') as f:
            return f.read()
    except FileNotFoundError:
        return None

# Good to fix vulnerability 4: Weak password hashing
def hash_password(password):
    # Using base64 instead of proper password hashing
    return base64.b64encode(password.encode()).decode()

# --- LIVE VULNERABILITIES (MUST FIX) ---

# These would typically be classified as "must_fix" but are included for reference only

# Flask route with template injection
# REMEDIATED: Using markupsafe.escape() to prevent XSS/template injection
@app.route('/dashboard')
def dashboard():
    name = request.args.get('name', 'Guest')
    # FIXED: Escape user input to prevent XSS/template injection
    template = f'''
    <div class="header">
        <h1>Welcome, {escape(name)}!</h1>
    </div>
    <div class="content">
        <p>Your dashboard content goes here.</p>
    </div>
    '''
    return render_template_string(template)

# Dangerous subprocess call with user input
# REMEDIATED: Using list arguments and input validation to prevent command injection
@app.route('/ping', methods=['POST'])
def ping_endpoint():
    host = request.form.get('host', '')
    # FIXED: Validate host input to ensure only valid hostnames/IPs are accepted
    if not re.match(r'^[a-zA-Z0-9.-]+$', host):
        return 'Invalid host', 400
    # FIXED: Use a list to avoid shell=True, preventing command injection
    result = subprocess.check_output(['ping', '-c', '1', host])
    return result.decode()

# --- DEAD CODE WITH VULNERABILITIES (FALSE POSITIVES) ---

# Dead code vulnerability 1: Unsafe deserialization
def legacy_load_user_preferences(data):
    # This function is not called anywhere in the codebase
    # Unsafe deserialization vulnerability
    return pickle.loads(base64.b64decode(data))

# Dead code vulnerability 2: Path traversal
def deprecated_get_file(filepath):
    # This function is not called anywhere
    # Direct path traversal vulnerability
    with open(filepath, 'r') as f:
        return f.read()

# Dead code vulnerability 3: Direct OS command execution
def admin_execute_command(command):
    # This function is not referenced anywhere
    # Direct command injection
    return os.system(command)

# Dead code vulnerability 4: Hardcoded credentials in dead code
def legacy_db_connect():
    # This function is unused
    username = "admin"
    password = "supersecretpassword123"
    conn = sqlite3.connect('old_database.db')
    # Would be vulnerable if used
    return conn

# --- ACTUAL APPLICATION ROUTES AND USAGE ---

@app.route('/')
def index():
    return "Welcome to the Demo App"

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '')
    password = request.form.get('password', '')
    
    # Using the vulnerable authentication function
    user = authenticate_user(username, password)
    
    if user:
        return f"Welcome, {user[1]}!"
    else:
        return "Invalid credentials"

@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username', '')
    password = request.form.get('password', '')
    email = request.form.get('email', '')
    
    # Using the vulnerable validation function
    if not validate_email(email):
        return "Invalid email format"
    
    # Using the vulnerable password hashing
    hashed_password = hash_password(password)
    
    conn = sqlite3.connect('users.db')
    c = conn.cursor()
    c.execute("INSERT INTO users (username, password, role) VALUES (?, ?, ?)",
              (username, hashed_password, 'user'))
    conn.commit()
    conn.close()
    
    return redirect(url_for('index'))

@app.route('/file')
def get_file():
    filename = request.args.get('name', '')
    content = read_user_file(filename)
    
    if content:
        return content
    else:
        return "File not found or access denied"

if __name__ == '__main__':
    initialize_db()
    app.run(debug=True)  # Debug mode in production is also a security issue
