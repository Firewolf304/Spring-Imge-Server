package org.example.security;

import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CustomUserDetails implements UserDetailsService {

    @Autowired
    private JdbcTemplate jdbcTemplate;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var users = jdbcTemplate.query("select id, username, role_id from users where username = ?", new Object[]{username}, (rs, rowNum) -> {
            UUID userId = UUID.fromString(rs.getString("id"));
            String role = getRoleById(rs.getInt("role_id")).toString();
            String password = getPasswordByUserId(userId);

            return User.withUsername(rs.getString("username"))
                    .password(password)
                    .roles(role)
                    .build();
        });

        if (users.isEmpty()) {
            throw new UsernameNotFoundException("User not found");
        }

        return users.get(0);
    }

    public String getPasswordByUserId(UUID userId) {
        return jdbcTemplate.queryForObject("select password_hash from passwords where user_id = ?", new Object[]{userId}, String.class);
    }

    public Object getRoleById(int roleId) {
        var val = jdbcTemplate.queryForList("select * from roles where id = ?", roleId);
        return val.get(0).get("role_id");
    }
    public int getAccessByUserID(UUID user_id) {
        var access = jdbcTemplate.queryForList("select * from users where id = ?", user_id).get(0).get("role_id");
        return (int)access;
    }

    public Object getUserIDBySession(UUID sess_id) {
        var user_id = jdbcTemplate.queryForList("select * from sessions where id = ?", sess_id).get(0).get("user_id");
        return user_id;
    }

    public UUID createSession(UUID userId) {
        var sessionId = UUID.randomUUID();
        var insert = "insert into sessions(id, user_id, comment) values (?, ?, '')";
        try {
            jdbcTemplate.update(insert, sessionId, userId);
        } catch (Exception e) {
            //jdbcTemplate.update(delete, userId);
            jdbcTemplate.update("delete from sessions where user_id = ?;", userId);
            jdbcTemplate.update(insert, sessionId, userId);
        }

        return sessionId;
    }
}
