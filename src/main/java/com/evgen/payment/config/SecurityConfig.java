package com.evgen.payment.config;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import com.evgen.payment.service.UserDetailsServiceImpl;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  private static List<String> clients = Collections.singletonList("google");

  private final UserDetailsServiceImpl userDetailsService;

  private final Environment env;

  @Autowired
  public SecurityConfig(UserDetailsServiceImpl userDetailsService, Environment env) {
    this.userDetailsService = userDetailsService;
    this.env = env;
  }

  @Autowired
  public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
    auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
  }

  @Bean
  public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(11);
  }

  //TODO: not working
  @Override
  protected void configure(final HttpSecurity http) throws Exception {
    http
        .authorizeRequests()
        .antMatchers("api/v1/registration", "api/v1/error-registration").anonymous()
        .antMatchers("api/v1/oauth_login").permitAll()
        .antMatchers(HttpMethod.POST, "api/v1/users").anonymous()
        .antMatchers("/**").access("hasRole('ROLE_USER')")
        .and()
        .formLogin()
        .loginPage("api/v1/login").permitAll()
        .defaultSuccessUrl("api/v1/users", true)
        .failureUrl("api/v1/login")
        .and()
        .logout()
        .logoutSuccessUrl("api/v1/login")
        .and()
        .oauth2Login()
        .loginPage("api/v1/login");
  }

  @Bean
  public OAuth2AuthorizedClientService authorizedClientService() {
    return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository());
  }

  @Bean
  public ClientRegistrationRepository clientRegistrationRepository() {
    List<ClientRegistration> registrations = clients.stream()
        .map(this::getRegistration)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    return new InMemoryClientRegistrationRepository(registrations);
  }

  private ClientRegistration getRegistration(String client) {
    String CLIENT_PROPERTY_KEY = "spring.security.oauth2.client.registration.";
    String clientId = env.getProperty(CLIENT_PROPERTY_KEY + client + ".client-id");
    String clientSecret = env.getProperty(CLIENT_PROPERTY_KEY + client + ".client-secret");

    if (client.equals("google")) {
      return CommonOAuth2Provider.GOOGLE
          .getBuilder(client)
          .clientId(clientId)
          .clientSecret(clientSecret)
          .build();
    }
    return null;
  }

}