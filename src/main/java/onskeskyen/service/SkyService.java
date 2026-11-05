package onskeskyen.service;

import jakarta.servlet.http.HttpSession;
import onskeskyen.model.UserDTO;
import onskeskyen.model.Wish;
import onskeskyen.repository.SkyRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import java.util.List;

@Service
public class SkyService {
    private final SkyRepository skyRepository;

    public SkyService(SkyRepository skyRepository) {
        this.skyRepository = skyRepository;
    }

    public void addWish(UserDTO userDTO, HttpSession sessions) {
        this.skyRepository.addWish(userDTO, sessions);
    }

    public int checkLogin(String username, String password, HttpSession session, String email) {
        int result = skyRepository.loginCheck(username, password, session, email);
        if (result == 1) {
            Integer userId = (Integer) session.getAttribute("user_id");
            if (userId != null) {
                getSessionWishesToDb(userId, session);
            }
        }
        return result;
    public void editWish(Wish wish, HttpSession session) {
        this.skyRepository.editWish(wish, session);
    }

    public Wish getWishByID(int wishID, HttpSession session) {
        return skyRepository.getWishByID(wishID, session);
    }

    public List<Wish> allWishesByUserID(HttpSession session) {
        return skyRepository.allWishesByUserID(session);
    }

    public List<String> allTags() {
        return skyRepository.allTags();
    }

    public int checkLogin(String username, String password, HttpSession session,String email){
        return skyRepository.loginCheck(username,password,session,email);
    }

    public int registerLogin(String username, String password, String email) {
        System.out.println("service called");
        return skyRepository.registerCheck(username, password, email);
    }

    @SuppressWarnings("unchecked")
    public void addQuickWish(String name, int price, List<String> tags, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("user_id");
        if (userId != null) {
            skyRepository.addQuickWish(userId, name, price, tags);
        } else {
            ArrayList<Wish> sessionWishes = (ArrayList<Wish>) session.getAttribute("quickWishes");
            if (sessionWishes == null) {
                sessionWishes = new ArrayList<>();
            }
            sessionWishes.add(new Wish(0, name, price, tags));
            session.setAttribute("quickWishes", sessionWishes);
        }
    }

    @SuppressWarnings("unchecked")
    public ArrayList<Wish> getQuickWishes(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("user_id");
        if (userId != null) {
            getSessionWishesToDb(userId, session);
            return skyRepository.getWishesByUserId(userId);
        }

        ArrayList<Wish> sessionWishes = (ArrayList<Wish>) session.getAttribute("quickWishes");
        return sessionWishes != null ? sessionWishes : new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private void getSessionWishesToDb(int userId, HttpSession session) {
        ArrayList<Wish> sessionWishes = (ArrayList<Wish>) session.getAttribute("quickWishes");
        if (sessionWishes != null && !sessionWishes.isEmpty()) {
            for (Wish wish : sessionWishes) {
                skyRepository.addQuickWish(userId, wish.getName(), (int) wish.getPrice(), wish.getTags());
            }
            session.removeAttribute("quickWishes");
        }
    }
}
