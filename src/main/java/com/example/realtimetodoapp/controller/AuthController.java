package com.example.realtimetodoapp.controller;

import io.ably.lib.rest.AblyRest;
import io.ably.lib.rest.Auth;
import io.ably.lib.types.AblyException;
import io.ably.lib.types.Capability;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
public class AuthController {
    private AblyRest ablyRest;

//    @Value("${eyJ0eXAiOiJKV1QiLCJ2ZXJzaW9uIjoxLCJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJkZmFmYmZiNi03MWMwLTRjMGQtOWJmOS1mNTljMmMxYzE3MGUiLCJpc3MiOiJhYmx5LmNvbSIsImlhdCI6MTY2NTc2NjEwOCwic3ViIjozMDI3MH0.FH8NVfkQnrCcntCOWfGEXwzm1mgVSbJu1f2xKFWNMMA}")
//    private void setAblyRest(String apiKey) throws AblyException {
//        ablyRest = new AblyRest(apiKey);
//    }

    public AuthController(@Value("${ABLY_API_KEY}") String apiKey) throws AblyException {
        ablyRest = new AblyRest(apiKey);
    }

    @RequestMapping("/auth")
    public String auth(HttpServletRequest request, HttpServletResponse response) throws AblyException {
        String username = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equalsIgnoreCase("username")) {
                    username = cookie.getValue();
                    break;
                }
            }
        }

        Auth.TokenParams tokenParams = getTokenParams(username);
        return createTokenRequest(tokenParams, response);
    }

    /* Set a cookie when the user logs in */
    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login(@RequestParam(name = "username", defaultValue = "anonymous") String username, HttpServletResponse response) throws IOException {
        /* Login the user without credentials. This is an over simplified authentication system to keep this tutorial simple */
        response.addCookie(new Cookie("username", username));
        response.sendRedirect("/");
        return "redirect:/";
    }

    /* Clear the cookie when the user logs outs */
    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equalsIgnoreCase("username")) {
                cookie.setValue(null);
                cookie.setMaxAge(0);
                cookie.setPath(request.getContextPath());
                response.addCookie(cookie);
            }
        }
        response.sendRedirect("/");
        return "redirect:/";
    }

    public Auth.TokenParams getTokenParams(String username) throws AblyException
    {
        Auth.TokenParams tokenParams = new Auth.TokenParams();
        tokenParams.capability = Capability.c14n("{ '*': ['subscribe'] }");
        if (username != null) {
            tokenParams.clientId = username;
        }
        return tokenParams;
    }

    public String createTokenRequest(Auth.TokenParams tokenParams, HttpServletResponse response) {
        Auth.TokenRequest tokenRequest;
        try {
            tokenRequest = ablyRest.auth.createTokenRequest(tokenParams, null);
            response.setHeader("Content-Type", "application/json");
            return tokenRequest.asJson();
        } catch (AblyException e) {
            response.setStatus(500);
            return "Error requesting token: " + e.getMessage();
        }
    }
}
