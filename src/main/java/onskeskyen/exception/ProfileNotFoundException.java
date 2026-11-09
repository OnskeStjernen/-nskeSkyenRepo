package onskeskyen.exception;

import jakarta.servlet.http.HttpSession;

public class ProfileNotFoundException extends RuntimeException {
    public ProfileNotFoundException(HttpSession session) {
        super("Profile with session id: " + session.getId() + ", could not be found");
    }
}
