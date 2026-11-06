package onskeskyen.repository;

import jakarta.servlet.http.HttpSession;
import onskeskyen.model.UserDTO;
import onskeskyen.model.Wish;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class SkyRepository {

    private final JdbcTemplate db;

    public SkyRepository(JdbcTemplate db) {
        this.db = db;
    }

    @Transactional
    public int addWish(UserDTO userDTO, HttpSession session) {

        Integer userID = (Integer) session.getAttribute("user_id");

        if(userID == null) {
            return -1;
        }

        String sqlWish = """
                INSERT INTO wish (name, price)
                VALUES(?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        db.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sqlWish, Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, userDTO.getName());
            ps.setDouble(2, userDTO.getPrice());

            return ps;
        }, keyHolder);

        Number wishID = keyHolder.getKey();

        if(wishID == null) {
            throw new IllegalStateException("Could not retrieve the wish id");
        }

        String sqlUserWishes = """
                INSERT INTO userWishes (user_id, wish_id) 
                VALUES (?, ?)
                """;

        db.update(sqlUserWishes, userID, wishID.intValue());


        String sqlTag = """
                SELECT tag_id
                FROM tags
                WHERE tag_name = ?
                """;

        String sqlTagsWish = """
               INSERT INTO tagsWish (tag_id, wish_id)
               VALUES (?,?)              
               """;

        // TODO: Guard against null tags list and missing tag rows in database
        if (userDTO.getTags() != null) {
            for (String tag : userDTO.getTags()) {
                List<Integer> tagIDs = db.queryForList(sqlTag, Integer.class, tag);
                if (!tagIDs.isEmpty()) {
                    db.update(sqlTagsWish, tagIDs.get(0), wishID.intValue());
                }
            }
        }

        return 1;
    }

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public int loginCheck(String username, String password, HttpSession session, String email) {
        if (conditionCheck(username, password, email)) return -1;

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

    public int registerCheck(String username, String password, String email) {
        System.out.println("repo called" + " " + username);

        if (conditionCheck(username, password, email)) return -1;

        String sql = "SELECT * FROM users WHERE userName = ?";
        List<Map<String, Object>> rows = db.queryForList(sql, username);
        String secureHash = passwordEncoder.encode(password);
        if (rows.isEmpty()) {
            String insert = "INSERT INTO users (userName, password,email) VALUES (?, ?,?)";
            db.update(insert, username, secureHash, email);
            return 1;
        }
        System.out.println("rows found!!");
        return -1;
    }

    private boolean conditionCheck(String username, String password, String email) {
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

    @Transactional
    public Wish addQuickWish(int userId, String name, int price, List<String> tags) {
        String insertWishSql = "INSERT INTO wish (name, price) VALUES (?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        db.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(insertWishSql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setInt(2, price);
            return ps;
        }, keyHolder);

        int exactWishId = keyHolder.getKey().intValue();

        String insertLinkSql = "INSERT INTO userWishes (user_id, wish_id) VALUES (?, ?)";

        db.update(insertLinkSql, userId, exactWishId);
        // TODO: Insert initial row in wishCounter (matching table case in schema.sql)
        String insertCount = "INSERT INTO wishCounter (wish_id, wish_count) VALUES (?, 1)";
        db.update(insertCount, exactWishId);
        return new Wish(exactWishId, name, price, tags);
    }

    public ArrayList<Wish> getWishesByUserId(int userId) {
        String selectWishesSql = """
                SELECT w.wish_id, w.name, w.price
                FROM wish w
                JOIN userWishes uw ON w.wish_id = uw.wish_id
                WHERE uw.user_id = ?
                """;

        List<Wish> wishes = db.query(selectWishesSql, (rs, rowNum) -> new Wish(
                rs.getInt("wish_id"),
                rs.getString("name"),
                rs.getInt("price"),
                new ArrayList<>()
        ), userId);

        return new ArrayList<>(wishes);
    }
    public boolean checkEmail(String email){
        String sql = "SELECT email FROM users WHERE email = ?";
        List<Map<String, Object>> rows = db.queryForList(sql, email);
        return !rows.isEmpty();

    }
    @Transactional
    public boolean ShareWish(String name, int price, String email) {
        // TODO: Reuse existing wish_id if wish name already exists, otherwise insert new wish and counter
        List<Integer> existingWishes = db.queryForList("SELECT wish_id FROM wish WHERE name = ?", Integer.class, name);
        int exactWishId;

        if (!existingWishes.isEmpty()) {
            exactWishId = existingWishes.get(0);
            db.update("UPDATE wishCounter SET wish_count = wish_count + 1 WHERE wish_id = ?", exactWishId);
        } else {
            String insertWishSql = "INSERT INTO wish (name, price) VALUES (?, ?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();

            db.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(insertWishSql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, name);
                ps.setInt(2, price);
                return ps;
            }, keyHolder);

            exactWishId = keyHolder.getKey().intValue();
            db.update("INSERT INTO wishCounter (wish_id, wish_count) VALUES (?, 1)", exactWishId);
        }

        String selectUser = "SELECT user_id FROM users WHERE email = ?";
        List<Map<String, Object>> list = db.queryForList(selectUser, email);
        int userId = ((Number) list.get(0).get("user_id")).intValue();

        // TODO: Avoid duplicate link if receiver already has this wish
        List<Integer> existingLink = db.queryForList(
                "SELECT userWish_id FROM userWishes WHERE user_id = ? AND wish_id = ?",
                Integer.class, userId, exactWishId
        );
        if (existingLink.isEmpty()) {
            String insertLinkSql = "INSERT INTO userWishes (user_id, wish_id) VALUES (?, ?)";
            db.update(insertLinkSql, userId, exactWishId);
        }
        return true;
    }
}
