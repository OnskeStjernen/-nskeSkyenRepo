package onskeskyen.service;

import jakarta.servlet.http.HttpSession;
import onskeskyen.repository.SkyRepository;
import org.springframework.stereotype.Service;

@Service
public class SkyService {
    private final SkyRepository skyRepository;

    public SkyService(SkyRepository skyRepository) {
        this.skyRepository = skyRepository;
    }
    public int checkLogin(String username, String password, HttpSession session,String email){
        return skyRepository.loginCheck(username,password,session,email);
    }

    public int registerLogin(String username, String password,String email){
        System.out.println("service called");
        return skyRepository.registerCheck(username,password,email);
    }
}
