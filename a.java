package com.security.test;

import java.io.*;
import java.sql.*;
import java.util.*;
import javax.servlet.http.*;
import java.security.MessageDigest;
import java.net.URL;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.*;

/**
 * Intentionally Vulnerable Java Application for AI-SAST Testing
 * Contains multiple vulnerability types with complex code flows
 */
public class VulnerableApplication {
    
    private static final String DB_URL = "jdbc:mysql://localhost:3306/testdb";
    private static final String ENCRYPTION_KEY = "hardcodedkey123"; // Hard-coded credentials
    private Connection dbConnection;
    private Map<String, UserSession> sessionCache = new HashMap<>();
    
    // VULN 1: SQL Injection with complex multi-method flow
    public List<User> searchUsers(HttpServletRequest request) throws SQLException {
        String searchTerm = request.getParameter("search");
        String role = request.getParameter("role");
        
        // Complex flow through multiple methods
        String sanitizedTerm = performBasicSanitization(searchTerm);
        String query = buildUserSearchQuery(sanitizedTerm, role);
        
        return executeUserQuery(query);
    }
    
    private String performBasicSanitization(String input) {
        // Insufficient sanitization - still vulnerable
        if (input == null) return "";
        return input.replace("'", "''"); // Only escapes single quotes, insufficient
    }
    
    private String buildUserSearchQuery(String term, String role) {
        StringBuilder query = new StringBuilder("SELECT * FROM users WHERE ");
        
        if (term != null && !term.isEmpty()) {
            query.append("name LIKE '%").append(term).append("%'");
        }
        
        if (role != null) {
            if (!query.toString().endsWith("WHERE ")) {
                query.append(" AND ");
            }
            query.append("role = '").append(role).append("'"); // SQL Injection point
        }
        
        return query.toString();
    }
    
    private List<User> executeUserQuery(String query) throws SQLException {
        List<User> users = new ArrayList<>();
        Statement stmt = dbConnection.createStatement();
        ResultSet rs = stmt.executeQuery(query); // Vulnerable execution
        
        while (rs.next()) {
            User user = new User();
            user.setId(rs.getInt("id"));
            user.setName(rs.getString("name"));
            user.setEmail(rs.getString("email"));
            users.add(user);
        }
        
        return users;
    }
    
    // VULN 2: Path Traversal with complex validation bypass
    public byte[] downloadFile(HttpServletRequest request) throws IOException {
        String fileName = request.getParameter("file");
        String category = request.getParameter("category");
        
        // Multi-stage validation that can be bypassed
        if (isValidFileName(fileName)) {
            String basePath = determineBasePath(category);
            String fullPath = constructFilePath(basePath, fileName);
            
            if (performSecurityCheck(fullPath)) {
                return readFileContents(fullPath);
            }
        }
        
        throw new SecurityException("Invalid file access");
    }
    
    private boolean isValidFileName(String fileName) {
        // Weak validation - can be bypassed
        if (fileName == null || fileName.isEmpty()) return false;
        
        // Blacklist approach - incomplete
        String[] blacklist = {"..", "~", "/etc/", "C:\\"};
        for (String blocked : blacklist) {
            if (fileName.contains(blocked)) {
                return false;
            }
        }
        return true;
    }
    
    private String determineBasePath(String category) {
        Map<String, String> basePaths = new HashMap<>();
        basePaths.put("documents", "/var/app/documents/");
        basePaths.put("images", "/var/app/images/");
        basePaths.put("reports", "/var/app/reports/");
        
        return basePaths.getOrDefault(category, "/var/app/public/");
    }
    
    private String constructFilePath(String base, String file) {
        // Vulnerable path construction
        return base + file; // No proper path validation
    }
    
    private boolean performSecurityCheck(String path) {
        // Insufficient security check
        return !path.contains("/etc/passwd"); // Easily bypassed
    }
    
    private byte[] readFileContents(String path) throws IOException {
        return Files.readAllBytes(Paths.get(path)); // Vulnerable read
    }
    
    // VULN 3: Command Injection with complex flow
    public String executeSystemCommand(HttpServletRequest request) throws IOException {
        String command = request.getParameter("cmd");
        String args = request.getParameter("args");
        String mode = request.getParameter("mode");
        
        CommandBuilder builder = new CommandBuilder();
        builder.setCommand(command);
        builder.setArguments(args);
        builder.setExecutionMode(mode);
        
        CommandValidator validator = new CommandValidator();
        if (validator.validate(builder)) {
            return executeCommand(builder.build());
        }
        
        return "Invalid command";
    }
    
    class CommandBuilder {
        private String cmd;
        private String arguments;
        private String mode;
        
        public void setCommand(String cmd) { this.cmd = cmd; }
        public void setArguments(String args) { this.arguments = args; }
        public void setExecutionMode(String mode) { this.mode = mode; }
        
        public String build() {
            if ("shell".equals(mode)) {
                return "/bin/sh -c " + cmd + " " + arguments; // Injection point
            }
            return cmd + " " + arguments;
        }
    }
    
    class CommandValidator {
        public boolean validate(CommandBuilder builder) {
            // Weak validation
            String[] allowedCommands = {"ls", "cat", "grep", "echo"};
            // Only checks command, not arguments
            return Arrays.asList(allowedCommands).contains(builder.cmd);
        }
    }
    
    private String executeCommand(String fullCommand) throws IOException {
        Process process = Runtime.getRuntime().exec(fullCommand); // Vulnerable
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream())
        );
        
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        
        return output.toString();
    }
    
    // VULN 4: XML External Entity (XXE) with preprocessing
    public Document parseUserXML(HttpServletRequest request) throws Exception {
        String xmlContent = request.getParameter("xml");
        
        // Multi-stage processing
        String preprocessed = preprocessXML(xmlContent);
        String validated = validateXMLStructure(preprocessed);
        
        return parseXMLDocument(validated);
    }
    
    private String preprocessXML(String xml) {
        // Some preprocessing that doesn't fix XXE
        return xml.replaceAll("<!\\[CDATA\\[", "").replaceAll("\\]\\]>", "");
    }
    
    private String validateXMLStructure(String xml) {
        // Checks structure but doesn't prevent XXE
        if (!xml.contains("<") || !xml.contains(">")) {
            throw new IllegalArgumentException("Invalid XML");
        }
        return xml;
    }
    
    private Document parseXMLDocument(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // XXE vulnerability - DTD processing enabled
        DocumentBuilder builder = factory.newDocumentBuilder();
        
        ByteArrayInputStream input = new ByteArrayInputStream(xml.getBytes());
        return builder.parse(input); // Vulnerable to XXE
    }
    
    // VULN 5: Insecure Deserialization with type confusion
    public Object deserializeUserData(HttpServletRequest request) throws Exception {
        String serializedData = request.getParameter("data");
        String objectType = request.getParameter("type");
        
        byte[] decodedData = Base64.getDecoder().decode(serializedData);
        
        ObjectTypeHandler handler = ObjectTypeHandlerFactory.getHandler(objectType);
        return handler.deserialize(decodedData);
    }
    
    interface ObjectTypeHandler {
        Object deserialize(byte[] data) throws Exception;
    }
    
    static class ObjectTypeHandlerFactory {
        public static ObjectTypeHandler getHandler(String type) {
            if ("user".equals(type)) {
                return new UserObjectHandler();
            } else if ("session".equals(type)) {
                return new SessionObjectHandler();
            }
            return new GenericObjectHandler();
        }
    }
    
    static class GenericObjectHandler implements ObjectTypeHandler {
        public Object deserialize(byte[] data) throws Exception {
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bis);
            return ois.readObject(); // Insecure deserialization
        }
    }
    
    static class UserObjectHandler implements ObjectTypeHandler {
        public Object deserialize(byte[] data) throws Exception {
            // Still vulnerable, just wrapped differently
            return new GenericObjectHandler().deserialize(data);
        }
    }
    
    static class SessionObjectHandler implements ObjectTypeHandler {
        public Object deserialize(byte[] data) throws Exception {
            return new GenericObjectHandler().deserialize(data);
        }
    }
    
    // VULN 6: LDAP Injection with complex filter building
    public List<String> searchLDAP(HttpServletRequest request) throws Exception {
        String userName = request.getParameter("username");
        String department = request.getParameter("dept");
        String role = request.getParameter("role");
        
        LDAPFilterBuilder filterBuilder = new LDAPFilterBuilder();
        filterBuilder.addCondition("cn", userName);
        filterBuilder.addCondition("department", department);
        filterBuilder.addCondition("role", role);
        
        String filter = filterBuilder.build();
        return performLDAPSearch(filter);
    }
    
    class LDAPFilterBuilder {
        private List<String> conditions = new ArrayList<>();
        
        public void addCondition(String attribute, String value) {
            if (value != null && !value.isEmpty()) {
                conditions.add("(" + attribute + "=" + value + ")"); // LDAP Injection
            }
        }
        
        public String build() {
            if (conditions.isEmpty()) {
                return "(objectClass=*)";
            }
            if (conditions.size() == 1) {
                return conditions.get(0);
            }
            return "(&" + String.join("", conditions) + ")";
        }
    }
    
    private List<String> performLDAPSearch(String filter) {
        // Simulated LDAP search
        return new ArrayList<>();
    }
    
    // VULN 7: DEAD CODE - Unreachable XSS vulnerability (False Positive)
    private String generateHTMLResponse(String userInput) {
        boolean isProduction = true;
        
        if (!isProduction) { // Always false - dead code
            // This XSS is unreachable
            return "<html><body>" + userInput + "</body></html>";
        }
        
        return sanitizeHTML(userInput);
    }
    
    private String sanitizeHTML(String input) {
        return input.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    }
    
    // VULN 8: DEAD CODE - Unreachable SQL Injection
    public void updateUserPreferences(int userId, String preferences) {
        final boolean USE_LEGACY_MODE = false;
        
        if (USE_LEGACY_MODE) { // Constant false - dead code
            try {
                String query = "UPDATE users SET prefs = '" + preferences + "' WHERE id = " + userId;
                Statement stmt = dbConnection.createStatement();
                stmt.executeUpdate(query); // Unreachable SQL Injection
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            // Safe implementation
            updatePreferencesSafely(userId, preferences);
        }
    }
    
    private void updatePreferencesSafely(int userId, String preferences) {
        try {
            PreparedStatement pstmt = dbConnection.prepareStatement(
                "UPDATE users SET prefs = ? WHERE id = ?"
            );
            pstmt.setString(1, preferences);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // VULN 9: Cryptographic Weakness with complex key derivation
    public String encryptSensitiveData(String data, String userSeed) throws Exception {
        KeyDerivationEngine engine = new KeyDerivationEngine();
        byte[] derivedKey = engine.deriveKey(userSeed);
        
        CryptoProcessor processor = new CryptoProcessor();
        return processor.encrypt(data, derivedKey);
    }
    
    class KeyDerivationEngine {
        public byte[] deriveKey(String seed) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5"); // Weak hash
                byte[] seedBytes = seed.getBytes();
                byte[] hash = md.digest(seedBytes);
                
                // Weak key derivation
                byte[] key = new byte[16];
                System.arraycopy(hash, 0, key, 0, 16);
                return key;
            } catch (Exception e) {
                return ENCRYPTION_KEY.getBytes();
            }
        }
    }
    
    class CryptoProcessor {
        public String encrypt(String data, byte[] key) throws Exception {
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); // ECB mode - weak
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] encrypted = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        }
    }
    
    // VULN 10: Race Condition with TOCTOU
    public boolean processFileUpload(HttpServletRequest request, String fileName) 
            throws IOException {
        File uploadFile = new File("/uploads/" + fileName);
        
        // Time-of-check
        if (uploadFile.exists()) {
            return false;
        }
        
        // Vulnerable gap - file could be created here
        
        // Time-of-use
        try (FileOutputStream fos = new FileOutputStream(uploadFile)) {
            byte[] content = getUploadedContent(request);
            fos.write(content);
        }
        
        return true;
    }
    
    private byte[] getUploadedContent(HttpServletRequest request) throws IOException {
        // Simulated upload content retrieval
        return new byte[1024];
    }
    
    // VULN 11: Session Fixation with complex session management
    public void authenticateUser(HttpServletRequest request, HttpServletResponse response) 
            throws Exception {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        
        // Multi-stage authentication
        UserCredentials creds = new UserCredentials(username, password);
        AuthenticationResult result = performAuthentication(creds);
        
        if (result.isSuccess()) {
            // Session Fixation - doesn't regenerate session ID
            HttpSession session = request.getSession();
            session.setAttribute("user", result.getUser());
            session.setAttribute("authenticated", true);
            
            // Additional vulnerable session handling
            String sessionToken = generateSessionToken(username);
            Cookie cookie = new Cookie("SESSION_TOKEN", sessionToken);
            cookie.setHttpOnly(false); // Missing HttpOnly flag
            cookie.setSecure(false); // Missing Secure flag
            response.addCookie(cookie);
        }
    }
    
    class UserCredentials {
        String username;
        String password;
        
        UserCredentials(String u, String p) {
            this.username = u;
            this.password = p;
        }
    }
    
    class AuthenticationResult {
        private boolean success;
        private User user;
        
        public boolean isSuccess() { return success; }
        public User getUser() { return user; }
        public void setSuccess(boolean s) { success = s; }
        public void setUser(User u) { user = u; }
    }
    
    private AuthenticationResult performAuthentication(UserCredentials creds) 
            throws SQLException {
        AuthenticationResult result = new AuthenticationResult();
        
        // Vulnerable password check
        String query = "SELECT * FROM users WHERE username = '" + 
                      creds.username + "' AND password = '" + creds.password + "'";
        
        Statement stmt = dbConnection.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        
        if (rs.next()) {
            User user = new User();
            user.setId(rs.getInt("id"));
            user.setName(rs.getString("name"));
            result.setSuccess(true);
            result.setUser(user);
        }
        
        return result;
    }
    
    private String generateSessionToken(String username) {
        // Weak token generation
        return username + "_" + System.currentTimeMillis();
    }
    
    // VULN 12: Server-Side Request Forgery (SSRF) with URL validation bypass
    public String fetchExternalResource(HttpServletRequest request) throws IOException {
        String url = request.getParameter("url");
        
        URLValidator validator = new URLValidator();
        if (validator.isValid(url)) {
            String processedURL = processURL(url);
            return fetchURL(processedURL);
        }
        
        return "Invalid URL";
    }
    
    class URLValidator {
        private List<String> blacklist = Arrays.asList(
            "localhost", "127.0.0.1", "192.168", "10.0", "172.16"
        );
        
        public boolean isValid(String url) {
            if (url == null || url.isEmpty()) return false;
            
            // Weak validation - can be bypassed
            for (String blocked : blacklist) {
                if (url.toLowerCase().contains(blocked)) {
                    return false;
                }
            }
            
            return url.startsWith("http://") || url.startsWith("https://");
        }
    }
    
    private String processURL(String url) {
        // URL processing that doesn't fix SSRF
        return url.trim();
    }
    
    private String fetchURL(String urlString) throws IOException {
        URL url = new URL(urlString);
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(url.openStream())
        );
        
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line);
        }
        
        return content.toString();
    }
    
    // Supporting classes
    class User {
        private int id;
        private String name;
        private String email;
        
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
    
    class UserSession {
        private String sessionId;
        private User user;
        private long timestamp;
        
        public String getSessionId() { return sessionId; }
        public User getUser() { return user; }
        public long getTimestamp() { return timestamp; }
    }
}
