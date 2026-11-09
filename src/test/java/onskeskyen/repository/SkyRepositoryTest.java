package onskeskyen.repository;
import onskeskyen.OnskeSkyenApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.jdbc.Sql;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(classes = OnskeSkyenApplication.class)
@Sql(
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:h2init.sql"}
)
class SkyRepositoryTest {
    @Autowired
    private SkyRepository skyRepository;
    @Test
    void loginCheck() {
        String email = "William@hotmail.com";
        MockHttpSession session = new MockHttpSession();
        // Register first so the password is stored as a BCrypt hash in H2
        skyRepository.registerCheck("William", "54321", email);
        int result = skyRepository.loginCheck("William", "54321", session, email);
        assertEquals(1, result);
        assertNotNull(session.getAttribute("user_id"));
    }
}