import os
import subprocess
import sqlite3
import pickle
import base64
import re
import logging
from flask import Flask, request, render_template_string, redirect, url_for

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
def authenticate_user(username, password):
    # Some attempt at sanitization but still vulnerable
    username = username.replace("'", "''")
    
    conn = sqlite3.connect('users.db')
    c = conn.cursor()
    # Still vulnerable to injection through password parameter
    query = f"SELECT id, username, role FROM users WHERE username = '{username}' AND password = '{password}'"
    logger.info(f"Executing query: {query}")  # Logging query for debugging (also a security issue)
    c.execute(query)
    user = c.fetchone()
    conn.close()
    return user

# Good to fix vulnerability 2: Weak regex for input validation
def validate_email(email):
    # Overly simplistic email validation that could allow some invalid formats
    pattern = r'.+@.+\..+'
    return re.match(pattern, email) is not None

# Good to fix vulnerability 3: Insecure file operations with some path normalization
def read_user_file(filename):
    # Attempts to prevent directory traversal but still vulnerable
    if '../' in filename or '/' in filename:
        return None
    
    # Still vulnerable to other traversal techniques
    try:
        with open(f"user_files/{filename}", 'r') as f:
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
@app.route('/dashboard')
def dashboard():
    name = request.args.get('name', 'Guest')
    # Template injection vulnerability
    template = f'''
    <div class="header">
        <h1>Welcome, {name}!</h1>
    </div>
    <div class="content">
        <p>Your dashboard content goes here.</p>
    </div>
    '''
    return render_template_string(template)

# Dangerous subprocess call with user input
@app.route('/ping', methods=['POST'])
def ping_endpoint():
    host = request.form.get('host', '')
    # Command injection vulnerability
    result = subprocess.check_output(f"ping -c 1 {host}", shell=True)
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
