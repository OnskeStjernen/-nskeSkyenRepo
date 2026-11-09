package onskeskyen.repository;

import jakarta.servlet.http.HttpSession;
import onskeskyen.model.Wish;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class SkyRepository {

    private final JdbcTemplate primaryDb;
    private volatile JdbcTemplate activeDb;
    private volatile boolean tablesInitialized = false;
    private volatile String dbStatusMessage = "Not initialized yet";

    public SkyRepository(JdbcTemplate db) {
        this.primaryDb = db;
    }

    public String getDbStatusMessage() {
        db();
        return dbStatusMessage;
    }

    private synchronized JdbcTemplate db() {
        if (activeDb != null && tablesInitialized) {
            return activeDb;
        }
        try {
            initSchema(primaryDb);
            activeDb = primaryDb;
            tablesInitialized = true;
            dbStatusMessage = "OK - Connected to primary database";
            return activeDb;
        } catch (Exception primaryEx) {
            System.out.println("Primary DB connection failed, switching to H2 fallback: " + primaryEx.getMessage());
            try {
                DriverManagerDataSource h2ds = new DriverManagerDataSource();
                h2ds.setDriverClassName("org.h2.Driver");
                h2ds.setUrl("jdbc:h2:mem:onskeskyen_fallback;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE");
                h2ds.setUsername("sa");
                h2ds.setPassword("");
                JdbcTemplate fallback = new JdbcTemplate(h2ds);
                initSchema(fallback);
                activeDb = fallback;
                tablesInitialized = true;
                dbStatusMessage = "Using H2 Fallback because MySQL failed: " + primaryEx.getMessage();
                return activeDb;
            } catch (Exception fallbackEx) {
                dbStatusMessage = "Both Primary and Fallback DB failed: " + primaryEx.getMessage();
                return primaryDb;
            }
        }
    }

    private void initSchema(JdbcTemplate jdbc) {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS users (
                user_id INT PRIMARY KEY AUTO_INCREMENT,
                userName VARCHAR(100) NOT NULL UNIQUE,
                email VARCHAR(100) NOT NULL UNIQUE,
                password VARCHAR(100) NOT NULL
            )
        """);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS wish (
                wish_id INT PRIMARY KEY AUTO_INCREMENT,
                name VARCHAR(100) NOT NULL,
                price INT
            )
        """);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS userWishes (
                userWish_id INT PRIMARY KEY AUTO_INCREMENT,
                user_id INT NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
                wish_id INT NOT NULL,
                FOREIGN KEY (wish_id) REFERENCES wish (wish_id) ON DELETE CASCADE
            )
        """);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS wishCounter (
                wishCount_id INT PRIMARY KEY AUTO_INCREMENT,
                wish_count INT DEFAULT 0,
                wish_id INT NOT NULL,
                FOREIGN KEY (wish_id) REFERENCES wish (wish_id)
            )
        """);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS reserve (
                reserve_id INT PRIMARY KEY AUTO_INCREMENT,
                user_id INT NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users (user_id),
                wish_id INT NOT NULL,
                FOREIGN KEY (wish_id) REFERENCES wish (wish_id)
            )
        """);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS tags (
                tag_id INT PRIMARY KEY AUTO_INCREMENT,
                tag_name VARCHAR(100) NOT NULL UNIQUE
            )
        """);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS tagsWish (
                tagsWish_id INT PRIMARY KEY AUTO_INCREMENT,
                tag_id INT NOT NULL,
                FOREIGN KEY (tag_id) REFERENCES tags (tag_id) ON DELETE CASCADE,
                wish_id INT NOT NULL,
                FOREIGN KEY (wish_id) REFERENCES wish (wish_id) ON DELETE CASCADE
            )
        """);
    }

    public int editWish(Wish w, HttpSession session) {
        Integer userID = (Integer) session.getAttribute("user_id");

        if (userID == null) {
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

        return db().update(sqlEditWish, w.getName(), w.getPrice(), w.getWishID(), userID);
    }

    public void updateTag(Wish w) {
        int wishID = w.getWishID();

        String deleteTags = """
                DELETE FROM tagsWish
                WHERE wish_id = ?
                """;
        db().update(deleteTags, wishID);

        String insertTags = """
                INSERT INTO tagsWish (tag_id, wish_id)
                SELECT tag_id, ?
                FROM tags
                WHERE tag_name = ?
                """;

        if (w.getTags() != null) {
            for (String tag_name : w.getTags()) {
                int rows = db().update(insertTags, wishID, tag_name);

                if (rows != 1) {
                    throw new IllegalArgumentException("Tag not found " + tag_name);
                }
            }
        }
    }

    public Wish getWishByID(int wishID, HttpSession session) {

        Integer userID = (Integer) session.getAttribute("user_id");

        if (userID == null) {
            throw new IllegalArgumentException("Could not retrive user information");
        }

        String sql = """
                SELECT w.wish_id, w.name, w.price
                FROM wish w
                JOIN userWishes uw ON uw.wish_id = w.wish_id 
                JOIN users u ON u.user_id = uw.user_id
                WHERE w.wish_id = ? AND uw.user_id = ?
                """;

        return db().queryForObject(sql, (rs, rowNun) -> new Wish(
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

        return db().query(sql, (rs, rowNum) -> rs.getString("tag_name"), wishID);
    }

    public List<String> allTags() {
        List<String> defaultTags = List.of("Bolig", "Elektronik", "B\u00f8ger", "T\u00f8j & Sko", "Oplevelser", "Sport", "K\u00f8kken");

        String sql = """
                SELECT tag_name
                FROM tags           
                """;

        try {
            List<String> tags = db().query(sql, (rs, rowNum) ->
                    rs.getString("tag_name"));

            if (tags.isEmpty()) {
                for (String tag : defaultTags) {
                    db().update("INSERT INTO tags (tag_name) VALUES (?)", tag);
                }
                return defaultTags;
            }

            return tags;
        } catch (Exception e) {
            System.out.println("Could not fetch tags from DB, using defaults: " + e.getMessage());
            return defaultTags;
        }
    }

    public int deleteWish(int wishID, HttpSession session) {

        Integer userID = (Integer) session.getAttribute("user_id");

        if (userID == null) {
            throw new IllegalArgumentException("Could not retrieve user information");
        }

        int removedLink = db().update("DELETE FROM userWishes WHERE wish_id = ? AND user_id = ?", wishID, userID);
        if (removedLink > 0) {
            db().update("DELETE FROM tagsWish WHERE wish_id = ?", wishID);
            db().update("DELETE FROM wishCounter WHERE wish_id = ?", wishID);
            db().update("DELETE FROM reserve WHERE wish_id = ?", wishID);
            return db().update("DELETE FROM wish WHERE wish_id = ?", wishID);
        }

        return 0;
    }

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public int loginCheck(String username, String password, HttpSession session, String email) {
        if (conditionCheck(username, password, email)) return -1;

        String sql = "SELECT * FROM users WHERE username = ?";
        List<Map<String, Object>> rows = db().queryForList(sql, username);

        if (rows.size() != 1 || !passwordEncoder.matches(password, (String) rows.get(0).get("password"))) {
            System.out.println("wrong password");
            return -1;
        }
        int userId = ((Number) rows.get(0).get("user_id")).intValue();

        session.setAttribute("user_id", userId);


        return 1;

    }

    public int registerCheck(String username, String password, String email) {
        System.out.println("repo called" + " " + username);

        if (conditionCheck(username, password, email)) return -1;

        String sql = "SELECT * FROM users WHERE userName = ?";
        List<Map<String, Object>> rows = db().queryForList(sql, username);
        String secureHash = passwordEncoder.encode(password);
        if (rows.isEmpty()) {
            String insert = "INSERT INTO users (userName, password,email) VALUES (?, ?,?)";
            db().update(insert, username, secureHash, email);
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

    public Wish addQuickWish(int userId, String name, int price, List<String> tags) {
        String insertWishSql = "INSERT INTO wish (name, price) VALUES (?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        db().update(connection -> {
            PreparedStatement ps = connection.prepareStatement(insertWishSql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setInt(2, price);
            return ps;
        }, keyHolder);

        int exactWishId = keyHolder.getKey().intValue();

        String insertLinkSql = "INSERT INTO userWishes (user_id, wish_id) VALUES (?, ?)";

        db().update(insertLinkSql, userId, exactWishId);

        String insertCount = "INSERT INTO wishCounter (wish_id, wish_count) VALUES (?, 1)";
        db().update(insertCount, exactWishId);

        String sqlTag = "SELECT tag_id FROM tags WHERE tag_name = ?";
        String sqlTagsWish = "INSERT INTO tagsWish (tag_id, wish_id) VALUES (?,?)";
        if (tags != null) {
            for (String tag : tags) {
                List<Integer> tagIDs = db().queryForList(sqlTag, Integer.class, tag);
                if (!tagIDs.isEmpty()) {
                    db().update(sqlTagsWish, tagIDs.get(0), exactWishId);
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

        List<Wish> wishes = db().query(selectWishesSql, (rs, rowNum) -> new Wish(
                rs.getInt("wish_id"),
                rs.getString("name"),
                rs.getInt("price"),
                getTagsByWishID(rs.getInt("wish_id"), null)
        ), userId);

        return new ArrayList<>(wishes);
    }

    public boolean checkEmail(String email) {
        String sql = "SELECT email FROM users WHERE email = ?";
        List<Map<String, Object>> rows = db().queryForList(sql, email);
        return !rows.isEmpty();
    }

    public boolean ShareWish(String name, int price, List<String> tags, String email) {

        List<Integer> existingWishes = db().queryForList("SELECT wish_id FROM wish WHERE name = ?", Integer.class, name);
        int exactWishId;

        if (!existingWishes.isEmpty()) {
            exactWishId = existingWishes.get(0);
            db().update("UPDATE wishCounter SET wish_count = wish_count + 1 WHERE wish_id = ?", exactWishId);
        } else {
            String insertWishSql = "INSERT INTO wish (name, price) VALUES (?, ?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();

            db().update(connection -> {
                PreparedStatement ps = connection.prepareStatement(insertWishSql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, name);
                ps.setInt(2, price);
                return ps;
            }, keyHolder);

            exactWishId = keyHolder.getKey().intValue();
            db().update("INSERT INTO wishCounter (wish_id, wish_count) VALUES (?, 1)", exactWishId);

            String sqlTag = "SELECT tag_id FROM tags WHERE tag_name = ?";
            String sqlTagsWish = "INSERT INTO tagsWish (tag_id, wish_id) VALUES (?,?)";
            if (tags != null) {
                for (String tag : tags) {
                    List<Integer> tagIDs = db().queryForList(sqlTag, Integer.class, tag);
                    if (!tagIDs.isEmpty()) {
                        db().update(sqlTagsWish, tagIDs.get(0), exactWishId);
                    }
                }
            }
        }

        String selectUser = "SELECT user_id FROM users WHERE email = ?";
        List<Map<String, Object>> list = db().queryForList(selectUser, email);
        int userId = ((Number) list.get(0).get("user_id")).intValue();

        List<Integer> existingLink = db().queryForList(
                "SELECT userWish_id FROM userWishes WHERE user_id = ? AND wish_id = ?",
                Integer.class, userId, exactWishId
        );
        if (existingLink.isEmpty()) {
            String insertLinkSql = "INSERT INTO userWishes (user_id, wish_id) VALUES (?, ?)";
            db().update(insertLinkSql, userId, exactWishId);
        }
        return true;
    }
}
