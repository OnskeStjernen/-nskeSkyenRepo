package onskeskyen.controller;

import jakarta.servlet.http.HttpSession;
import onskeskyen.model.Wish;
import onskeskyen.service.SkyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SkyController.class)
class SkyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SkyService skyService;

    @Test
    void shouldReturnWishes() throws Exception {

        MockHttpSession session = new MockHttpSession();

        ArrayList<Wish> wishes = new ArrayList<>();

        when(skyService.getQuickWishes(session)).thenReturn(wishes);

        when(skyService.allTags()).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/quickwish").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("QuickWish"))
                .andExpect(model().attributeExists("wishes"))
                .andExpect(model().attribute("wishes", wishes))
                .andExpect(model().attributeExists("allTags"))
                .andExpect(model().attribute("isLoggedIn", false));

        verify(skyService).getQuickWishes(session);
        verify(skyService).allTags();
    }

}