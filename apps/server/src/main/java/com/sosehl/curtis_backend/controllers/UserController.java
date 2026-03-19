package com.sosehl.curtis_backend.controllers;

import com.sosehl.curtis_backend.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
public class UserController {

    final UserService userService;

    UserController() {
        this.userService = new UserService();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void redirectToLogin() {}
}
