package com.krishiconnect.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Sends browser routes like /equipment/3 to the React app so a page refresh doesn't 404. */
@Controller
public class SpaController {

    @GetMapping({"/login", "/register", "/profile", "/nearby", "/equipment/**", "/specialists/**",
            "/requirements/**", "/bookings/**", "/provider/**", "/notifications", "/seasonal", "/admin/**"})
    public String forward() {
        return "forward:/index.html";
    }
}
