package onskeskyen.controller;


import jakarta.servlet.http.HttpSession;
import onskeskyen.model.UserDTO;

import onskeskyen.model.Wish;
import onskeskyen.model.WishList;
import onskeskyen.service.SkyService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;


@Controller
public class SkyController {
    private final SkyService skyservice;


    @GetMapping("/")
    public String home() {
        return "Homepage";
    }

    public SkyController(SkyService skyservice) {
        this.skyservice = skyservice;
    }

    @GetMapping("/addWish")
    public String addWish(Model model, HttpSession session) {
        Integer userID = (Integer) session.getAttribute("user_id");

        if(userID == null) {
            return "redirect:/login";
        }
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

        return "redirect:/";
    }

    @PostMapping("/quickShareWish")
    public String quickShareWish(@RequestParam String name,
                                 @RequestParam int price,
                                 @RequestParam(required = false) List<String> tags,
                                 @RequestParam String email,HttpSession session){
        Integer userID = (Integer) session.getAttribute("user_id");

        if(userID == null) {
            return "redirect:/login";
        }

        skyservice.quickShareService(name,price,tags,email);


    }


    @PostMapping("/login")
    public String login(@RequestParam(required = false) String username, @RequestParam(required = false) String password,@RequestParam(required = false) String email, HttpSession session, ModelMap model){
        if(skyservice.checkLogin(username,password,session,email) == 1) {
            model.addAttribute("username", username);
            System.out.println("login succsess");
            return "redirect:/";
        }else {
            System.out.println("login failed");
            return "redirect:/";
        }
    }

    @PostMapping("/register")
    public String register(@RequestParam(required = false) String username, @RequestParam(required = false) String password,@RequestParam(required = false) String email, ModelMap model){
        if(skyservice.registerLogin(username,password,email) == 1) {
            System.out.println("user made");
            model.addAttribute("username", username);
            return "login";
        }else{
            System.out.println("error");
            model.addAttribute("error", "Username already taken");
            return "redirect:/homepage";
        }
    }

    @GetMapping("/login")
    public String getLogin(ModelMap modelMap){
        return "login";
    }

    @GetMapping("/register")
    public String getRegister(ModelMap modelMap){

        return "register";
    }

    @GetMapping({"/quickwish", "/quickWish", "/QuickWish"})
    public String quickAdd(ModelMap model, HttpSession session) {
        ArrayList<Wish> wishItems = skyservice.getQuickWishes(session);
        model.addAttribute("wishes", wishItems);
        return "QuickWish";
    }

    @PostMapping("/quickAddWish")
    public String quickAddWish(@RequestParam String name,
                               @RequestParam int price,
                               @RequestParam(required = false) List<String> tags,
                               HttpSession session) {
        skyservice.addQuickWish(name, price, tags, session);
        return "redirect:/quickwish";
    }

}
