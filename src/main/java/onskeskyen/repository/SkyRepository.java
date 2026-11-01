package onskeskyen.repository;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.List;
import java.util.Map;

@Repository
public class SkyRepository {

    private final JdbcTemplate db;

    public SkyRepository(JdbcTemplate db) {
        this.db = db;
    }
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    public int loginCheck(String username, String password, HttpSession session,String email) {
        if(conditionCheck(username, password,email)) return -1;

        String sql = "SELECT * FROM users WHERE username = ?";
        List<Map<String, Object>> rows = db.queryForList(sql, username);

        if (rows.size() != 1 || !passwordEncoder.matches(password, (String) rows.get(0).get("password"))) {
            System.out.println("wrong password");
            return -1;
        }
        int userId = (int) rows.get(0).get("user_id");

        session.setAttribute("user_id", userId);


        return 1;

    }

    public int registerCheck(String username, String password,String email) {
        System.out.println("repo called" + " " + username);

        if(conditionCheck(username, password,email)) return -1;

        String sql = "SELECT * FROM users WHERE userName = ?";
        List<Map<String, Object>> rows = db.queryForList(sql, username);
        String secureHash = passwordEncoder.encode(password);
        if (rows.isEmpty()) {
            String insert = "INSERT INTO users (userName, password,email) VALUES (?, ?,?)";
            db.update(insert, username, secureHash,email);
            return 1;

        }
        System.out.println("rows found!!");
        return -1;

    }
    private boolean conditionCheck(String username, String password,String email) {
        if (username == null || username.trim().isEmpty()) {
            System.out.println("username");
            return true;
        }
        if (password == null || password.trim().isEmpty()) {
            System.out.println("password");
            return true;
        }

        if (email == null || email.trim().isEmpty()) {
            System.out.println("email");
            return true;
        }
        return false;
    }
}
