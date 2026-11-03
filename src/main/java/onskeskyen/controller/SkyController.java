package onskeskyen.controller;


import jakarta.servlet.http.HttpSession;
import onskeskyen.model.UserDTO;
import onskeskyen.service.SkyService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class SkyController {
    private final SkyService skyservice;

    public SkyController(SkyService skyservice) {
        this.skyservice = skyservice;
    }

    @GetMapping("/")
    public String home(){
        return "home";
    }

    @GetMapping("/addWish")
    public String addWish(Model model) {
        UserDTO userWish = new UserDTO();
        model.addAttribute("addedUserWish", userWish);
        model.addAttribute("allTags");
        return "addForm";
    }

    @PostMapping("/saveWish")
    public String saveWish(@ModelAttribute UserDTO userDTO, HttpSession session) {

        Integer userID = (Integer) session.getAttribute("user_id");

        if(userID == null) {
            return "redirect:/login";
        }

        skyservice.addWish(userDTO, session);

        return "redirect:/homepage";
    }

    //TODO getMapping for /login og /register

    @PostMapping("/login")
    public String login(@RequestParam(required = false) String username, @RequestParam(required = false) String password,@RequestParam(required = false) String email, HttpSession session, ModelMap model){
        if(skyservice.checkLogin(username,password,session,email) == 1) {
            model.addAttribute("username", username);
            //return getAllAttractions(model);
        }else{
            //return apology(model);
        }
        //TODO TEMP
        return null;
    }

    @PostMapping("/register")
    public String register(@RequestParam(required = false) String username, @RequestParam(required = false) String password,@RequestParam(required = false) String email, ModelMap model){
        if(skyservice.registerLogin(username,password,email) == 1) {
            System.out.println("user made");
            model.addAttribute("username", username);
            //return "login";
        }else{
            System.out.println("error");
            model.addAttribute("error", "Username already taken");
            //return apology(model);
        }
        //TODO TEMP
        return null;
    }


}
