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

    public List<Wish> getAllWishes(HttpSession session) {

        Integer userID = (Integer) session.getAttribute("user_id");

        if(userID == null) {
            throw new IllegalArgumentException("could not find user information");
        }

        String sql = """
               SELECT w.wish_id, w.name, w.price
               FROM wish w
               JOIN userWishes uw ON uw.wish_id = w.wish_id
               WHERE uw.user_id = ?
                """;

        List<Wish> wishes = db.query(sql, (rs, rowNum) -> new Wish(
                rs.getInt("wish_id"),
                rs.getString("name"),
                rs.getFloat("price"),
                getTagsByWishID(rs.getInt("wish_id"), session)

        ), userID);

       return wishes;
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

        for(String tag : userDTO.getTags()) {

            Integer tagID = db.queryForObject(sqlTag, Integer.class, tag);

            db.update(sqlTagsWish, tagID, wishID.intValue());
        }

        return 1;
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

        for(String tag_name : w.getTags()) {
            int rows = db.update(insertTags, wishID, tag_name);

            if(rows != 1) {
                throw new IllegalArgumentException("Tag not found " + tag_name);
            }
        }
    }

    public List<Wish> allWishesByUserID(HttpSession session) {

        Integer userID = (Integer) session.getAttribute("user_id");

        if(userID == null) {
            throw new IllegalArgumentException("can not retrieve user information");
        }

        String sql = """
                SELECT w.wish_id, w.name, w.price 
                FROM wish w
                JOIN userWishes uw ON w.wish_id = uw.wish_id
                WHERE uw.user_id = ?
                """;

        return db.query(sql, (rs, rowNum) -> new Wish(
                rs.getInt("wish_id"),
                rs.getString("name"),
                rs.getFloat("price"),
                getTagsByWishID(rs.getInt("wish_id"), session)
        ), userID);
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

        String sql = """
                SELECT tag_name
                FROM tags           
                """;

        List<String> tags = db.query(sql, (rs, rowNum) ->
                rs.getString("tag_name"));

        return tags;
    }

    @Transactional
    public int deleteWish(int wishID, HttpSession session) {

        Integer userID = (Integer) session.getAttribute("user_id");

        if(userID == null) {
            throw new IllegalArgumentException("Could not retrieve user information");
        }

        String sql = """
                DELETE w FROM wish w
                JOIN userWishes uw ON uw.wish_id = w.wish_id
                WHERE w.wish_id = ? AND uw.user_id = ?
                """;

        return db.update(sql, wishID, userID);
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
}
