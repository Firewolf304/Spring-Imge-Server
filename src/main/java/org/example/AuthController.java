package org.example;

import jakarta.servlet.http.HttpServletRequest;
import org.example.repository.UserRepository;
import org.example.security.CustomUserDetails;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.bind.annotation.*;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@RestController
public class AuthController {
    //============== local
    Logger logger = LoggerFactory.getLogger(App.class);

    //============== local

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CustomUserDetails userDetailsService;

    @GetMapping("/users")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> users() {
        return new ResponseEntity<>( jdbcTemplate.queryForList("select * from users;"), HttpStatus.OK);
    }

    @PostMapping("/session")
    public ResponseEntity<String> session(@RequestParam(name = "id", required = false, defaultValue = "0") UUID user_id, @RequestHeader HttpHeaders headers) {
        var user = jdbcTemplate.queryForList("select * from sessions where id = ?", user_id);
        return new  ResponseEntity<>(user.get(0).get("id").toString(), HttpStatus.OK);
    }

    @PostMapping("/auth_method")
    public ResponseEntity<String> login(@RequestHeader HttpHeaders headers, @RequestBody(required = true) String body, HttpServletRequest request) {
        try {
            var json = new JSONObject(body);
            var username = json.get("login").toString();
            var password = json.get("password").toString();

            var users = jdbcTemplate.queryForList("SELECT * FROM users WHERE username = ?", username);
            if (users.isEmpty()) {
                throw new Exception("Bad auth");
            }

            var userId = UUID.fromString(users.get(0).get("id").toString());
            var passwords = jdbcTemplate.queryForList("SELECT * FROM passwords WHERE user_id = ?", userId);
            if (passwords.isEmpty() || !password.equals(passwords.get(0).get("password_hash"))) {
                throw new Exception("Bad auth");
            }

            var sessionId = userDetailsService.createSession(userId);
            var user = users.get(0);
            var resp = new JSONObject();
            resp.put("level", user.get("role_id"));
            resp.put("user", user.get("username"));
            resp.put("user_id", user.get("id"));
            resp.put("id_session", sessionId);

            return new ResponseEntity<>(resp.toString(), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("error", HttpStatus.BAD_REQUEST);
        }
        /*try {
            var json = new JSONObject(body);
            var user = jdbcTemplate.queryForList("select * from users where username = '" + json.get("login") + "';"); // injection warning!
            var passwords = jdbcTemplate.queryForList("select * from passwords where user_id = '" + user.get(0).get("id") + "';"); // injection warning!
            var resp = new JSONObject();
            if (passwords.get(0).get("password_hash").toString().equals(json.get("password").toString())) {
                try {
                    jdbcTemplate.update("insert into sessions(user_id, comment) values('" + user.get(0).get("id") + "', '');");  // injection warning!
                } catch (Exception e) {
                    jdbcTemplate.update("delete from sessions where user_id = '" + user.get(0).get("id") + "';");  // injection warning!
                    jdbcTemplate.update("insert into sessions(user_id, comment) values('" + user.get(0).get("id") + "', '');");  // injection warning!
                }
                var data = jdbcTemplate.queryForList("select * from users where id = '"  + user.get(0).get("id") + "';");  // injection warning!
                resp.put("level", data.get(0).get("role_id"));
                resp.put("user", data.get(0).get("username"));
                resp.put("user_id", data.get(0).get("id"));



                data = jdbcTemplate.queryForList("select * from sessions where user_id = '"  + user.get(0).get("id") + "';");  // injection warning!
                resp.put("id_session", data.get(0).get("id"));


            } else {
                throw new Exception("Bad auth");
            }
            var res = resp.toString();
            return new ResponseEntity<>(res, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error auth: " + e.getMessage());
            return new ResponseEntity<>("error", HttpStatus.BAD_REQUEST);
        }*/
    }
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestHeader HttpHeaders headers, @RequestBody(required = true) String body) {
        try {
            var json = new JSONObject(body);
            var username = json.get("login").toString();
            var password = json.get("password").toString();

            jdbcTemplate.update("insert into users(username, role_id) values('" + username + "', 3);");
            var newuser = jdbcTemplate.queryForList("select * from users where username = '" + username + "';");
            jdbcTemplate.update("insert into passwords(user_id, password_hash) values('" + newuser.get(0).get("id") + "', '" + password + "');");

            var users = jdbcTemplate.queryForList("SELECT * FROM users WHERE username = ?", username);
            if (users.isEmpty()) {
                throw new Exception("Bad auth");
            }

            var userId = UUID.fromString(users.get(0).get("id").toString());
            var passwords = jdbcTemplate.queryForList("SELECT * FROM passwords WHERE user_id = ?", userId);
            if (passwords.isEmpty() || !password.equals(passwords.get(0).get("password_hash"))) {
                throw new Exception("Bad auth");
            }

            var sessionId = userDetailsService.createSession(userId);
            var user = users.get(0);
            var resp = new JSONObject();
            resp.put("level", user.get("role_id"));
            resp.put("user", user.get("username"));
            resp.put("user_id", user.get("id"));
            resp.put("id_session", sessionId);
            return new ResponseEntity<>(resp.toString(), HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error auth: " + e.getMessage());
            return new ResponseEntity<>("error", HttpStatus.BAD_REQUEST);
        }
    }
    @RequestMapping( value = "/logout", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> logout(@CookieValue("id") String id) {
        try {
            jdbcTemplate.update("delete from sessions where id = ?;", id);
            return new ResponseEntity<>("ok", HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error", HttpStatus.BAD_REQUEST);
        }
    }
}
