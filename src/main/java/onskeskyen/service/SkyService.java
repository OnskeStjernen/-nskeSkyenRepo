package onskeskyen.service;

import jakarta.servlet.http.HttpSession;
import onskeskyen.model.UserDTO;
import onskeskyen.model.Wish;
import onskeskyen.repository.SkyRepository;
import org.springframework.stereotype.Service;

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

    public int registerLogin(String username, String password,String email){
        System.out.println("service called");
        return skyRepository.registerCheck(username,password,email);
    }
}
