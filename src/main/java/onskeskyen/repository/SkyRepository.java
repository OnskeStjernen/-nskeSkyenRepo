package onskeskyen.repository;

import jakarta.servlet.http.HttpSession;
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
    public int editWish(Wish w, HttpSession session) {
        Integer userID = (Integer) session.getAttribute("user_id");

        if(userID == null) {
            return -1;
        }

        String sqlEditWish = """
                UPDATE wish
                SET name = ?, price = ?
                WHERE wish_id = ?
                AND EXISTS (
                    SELECT 1
                    FROM userWishes
                    WHERE userWishes.wish_id = wish.wish_id
                    AND userWishes.user_id = ?
                ) 
                """;

        updateTag(w);

        return db.update(sqlEditWish, w.getName(), w.getPrice(), w.getWishID(), userID);
    }

    @Transactional
    public void updateTag(Wish w) {
        int wishID = w.getWishID();

        String deleteTags = """
                DELETE FROM tagsWish
                WHERE wish_id = ?
                """;
        db.update(deleteTags, wishID);

        String insertTags = """
                INSERT INTO tagsWish (tag_id, wish_id)
                SELECT tag_id, ?
                FROM tags
                WHERE tag_name = ?
                """;

        if (w.getTags() != null) {
            for(String tag_name : w.getTags()) {
                int rows = db.update(insertTags, wishID, tag_name);

                if(rows != 1) {
                    throw new IllegalArgumentException("Tag not found " + tag_name);
                }
            }
        }
    }

    public Wish getWishByID(int wishID, HttpSession session) {

        Integer userID = (Integer) session.getAttribute("user_id");

        if(userID == null) {
            throw new IllegalArgumentException("Could not retrive user information");
        }

        String sql = """
                SELECT w.wish_id, w.name, w.price
                FROM wish w
                JOIN userWishes uw ON uw.wish_id = w.wish_id 
                JOIN users u ON u.user_id = uw.user_id
                WHERE w.wish_id = ? AND uw.user_id = ?
                """;

        return db.queryForObject(sql, (rs, rowNun) -> new Wish(
                rs.getInt("wish_id"),
                rs.getString("name"),
                rs.getFloat("price"),
                getTagsByWishID(rs.getInt("wish_id"), session)
        ), wishID, userID);
    }

    public List<String> getTagsByWishID(int wishID, HttpSession session) {

        String sql = """
                SELECT tag_name
                FROM tags t
                JOIN tagsWish tw ON tw.tag_id = t.tag_id
                WHERE wish_id = ?               
                """;

        return db.query(sql, (rs, rowNum) -> rs.getString("tag_name"), wishID);
    }

    public List<String> allTags() {
        List<String> defaultTags = List.of("Bolig", "Elektronik", "B\u00f8ger", "T\u00f8j & Sko", "Oplevelser", "Sport", "K\u00f8kken");

        String sql = """
                SELECT tag_name
                FROM tags           
                """;

        try {
            List<String> tags = db.query(sql, (rs, rowNum) ->
                    rs.getString("tag_name"));

            if (tags.isEmpty()) {
                for (String tag : defaultTags) {
                    db.update("INSERT INTO tags (tag_name) VALUES (?)", tag);
                }
                return defaultTags;
            }

            return tags;
        } catch (Exception e) {
            System.out.println("Could not fetch tags from DB, using defaults: " + e.getMessage());
            return defaultTags;
        }
    }

    @Transactional
    public int deleteWish(int wishID, HttpSession session) {

        Integer userID = (Integer) session.getAttribute("user_id");

        if(userID == null) {
            throw new IllegalArgumentException("Could not retrieve user information");
        }


        int removedLink = db.update("DELETE FROM userWishes WHERE wish_id = ? AND user_id = ?", wishID, userID);
        if (removedLink > 0) {
            db.update("DELETE FROM tagsWish WHERE wish_id = ?", wishID);
            db.update("DELETE FROM wishCounter WHERE wish_id = ?", wishID);
            db.update("DELETE FROM reserve WHERE wish_id = ?", wishID);
            return db.update("DELETE FROM wish WHERE wish_id = ?", wishID);
        }

        return 0;
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

        String insertCount = "INSERT INTO wishCounter (wish_id, wish_count) VALUES (?, 1)";
        db.update(insertCount, exactWishId);

        String sqlTag = "SELECT tag_id FROM tags WHERE tag_name = ?";
        String sqlTagsWish = "INSERT INTO tagsWish (tag_id, wish_id) VALUES (?,?)";
        if (tags != null) {
            for (String tag : tags) {
                List<Integer> tagIDs = db.queryForList(sqlTag, Integer.class, tag);
                if (!tagIDs.isEmpty()) {
                    db.update(sqlTagsWish, tagIDs.get(0), exactWishId);
                }
            }
        }

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
                getTagsByWishID(rs.getInt("wish_id"), null)
        ), userId);

        return new ArrayList<>(wishes);
    }
    public boolean checkEmail(String email){
        String sql = "SELECT email FROM users WHERE email = ?";
        List<Map<String, Object>> rows = db.queryForList(sql, email);
        return !rows.isEmpty();

    }
    @Transactional
    public boolean ShareWish(String name, int price, List<String> tags, String email) {

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

            String sqlTag = "SELECT tag_id FROM tags WHERE tag_name = ?";
            String sqlTagsWish = "INSERT INTO tagsWish (tag_id, wish_id) VALUES (?,?)";
            if (tags != null) {
                for (String tag : tags) {
                    List<Integer> tagIDs = db.queryForList(sqlTag, Integer.class, tag);
                    if (!tagIDs.isEmpty()) {
                        db.update(sqlTagsWish, tagIDs.get(0), exactWishId);
                    }
                }
            }
        }

        String selectUser = "SELECT user_id FROM users WHERE email = ?";
        List<Map<String, Object>> list = db.queryForList(selectUser, email);
        int userId = ((Number) list.get(0).get("user_id")).intValue();


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
