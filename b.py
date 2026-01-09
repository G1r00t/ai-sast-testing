import os
import re

# === DEAD CODE ===
# This function is never called. It contains a hardcoded credential.
def unused_login():
    username = "admin"
    password = "admin123"  # Vulnerability: Hardcoded password (DEAD CODE, not exploitable)
    print("Logging in with", username)

# === DEAD CODE ===
# Not used anywhere.
def insecure_eval():
    user_input = "2 + 2"
    result = eval(user_input)  # Vulnerability: Use of eval (dead, but risky if reused)
    print("Eval result:", result)

# === LIVE CODE ===
def get_user_profile(user_id):
    # No input validation on user_id
    print("Fetching profile for user:", user_id)

    # Simulate command injection risk if this were used more dangerously
    os.system(f"echo {user_id}")  # Vulnerability: Potential shell injection (low severity here)

# === LIVE CODE ===
def check_password(password):
    # Vulnerability: Weak regex check instead of proper password hashing or validation
    if re.match(r"^[a-zA-Z0-9]{6,}$", password):
        print("Password format looks okay")
    else:
        print("Password format invalid")

# === LIVE CODE ===
def display_user_comment(comment):
    # Vulnerability: Directly prints user content (may be fine in CLI, but risky in web context)
    print(f"User says: {comment}")

# === MAIN DRIVER ===
if __name__ == "__main__":
    user_id = input("Enter user ID: ")
    get_user_profile(user_id)

    password = input("Enter password: ")
    check_password(password)

    comment = input("Leave a comment: ")
    display_user_comment(comment)
