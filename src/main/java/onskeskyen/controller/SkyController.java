package onskeskyen.controller;


import onskeskyen.model.UserDTO;
import onskeskyen.service.SkyService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;


@Controller
public class SkyController {
    private final SkyService service;

    public SkyController(SkyService skyService) {
        this.service = skyService;
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

}
