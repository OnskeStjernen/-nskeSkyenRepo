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


@Controller
public class SkyController {
    private final SkyService skyservice;

    public SkyController(SkyService skyservice) {
        this.skyservice = skyservice;
    }

    @GetMapping({"/", "/homepage"})
    public String homepage() {
        return "Homepage";
    }

    @GetMapping("/addWish")
    public String addWish(Model model) {
        UserDTO userWish = new UserDTO();
        model.addAttribute("addedUserWish", userWish);
        model.addAttribute("allTags");
        return "addForm";
    }

    @PostMapping("/saveWish")
    public String saveWish(@ModelAttribute UserDTO userWish) {

        return "redirect:/";
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

    @GetMapping("quickWish")
    public String quickAdd(@RequestParam WishList wish,ModelMap model){
        if(wish == null){
            WishList wishes = new WishList(null);
            model.addAttribute("item",wishes);
            return "";
        }else{

        }

    }

    @PostMapping("/quickAddWish")
    public String quickAddWish(@RequestParam String name,
                               @RequestParam int price,
                               HttpSession session) {
        Integer userId = (Integer) session.getAttribute("user_id");
        if (userId == null) {
            return "redirect:/login"; // Not logged in yet
        }
        // Call skyservice to save the wish for this userId
        // skyservice.addRecommendedWish(userId, name, price);
        return "redirect:/quickWish";
    }

}
