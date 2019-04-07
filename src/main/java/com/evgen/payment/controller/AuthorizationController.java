package com.evgen.payment.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.HttpClientErrorException;

import com.evgen.payment.model.User;
import com.evgen.payment.repository.UserRepository;
import com.evgen.payment.service.api.UserCreateService;
import com.evgen.payment.utils.Oauth2Utils;

@Controller
public class AuthorizationController {

  private final UserCreateService userCreateServiceImpl;
  private final UserRepository userRepository;
  private final Oauth2Utils oauth2Utils;
  private Map<String, String> oauth2AuthenticationUrls = new HashMap<>();

  @Autowired
  public AuthorizationController(
      UserCreateService userCreateServiceImpl, Oauth2Utils oauth2Utils, UserRepository userRepository) {
    this.userCreateServiceImpl = userCreateServiceImpl;
    this.userRepository = userRepository;
    this.oauth2Utils = oauth2Utils;
  }

  @GetMapping("api/v1/users")
  public ResponseEntity<User> retrieveUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    try {
      User user = getUserByUserNameOrThrowException(authentication.getName());
      return ResponseEntity.ok().body(user);
    } catch (HttpClientErrorException e) {
      if (authentication == null) {
        return ResponseEntity.ok().body(null);
      }
      User user = userCreateServiceImpl.createUserFromGoogle(authentication.getName());
      return ResponseEntity.ok().body(user);
    }
  }

  @RequestMapping("api/v1/login")
  public String login(Model model) {
    oauth2Utils.setOauth2AuthenticationUrls(oauth2AuthenticationUrls);
    model.addAttribute("urls", oauth2AuthenticationUrls);

    return "login";
  }

  @RequestMapping("api/v1/login-error")
  public String loginError(Model model) {
    oauth2Utils.setOauth2AuthenticationUrls(oauth2AuthenticationUrls);
    model.addAttribute("urls", oauth2AuthenticationUrls);
    model.addAttribute("loginError", true);

    return "login";
  }

  private User getUserByUserNameOrThrowException(String userName) throws RuntimeException {
    return userRepository.findByUserName(userName)
        .orElseThrow(() -> new RuntimeException(String.format("User with name %s not found", userName)));
  }
}