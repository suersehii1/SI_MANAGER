package com.sipm.user;

import com.sipm.user.dto.UserSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserLookupController {
    private final UserService userService;

    public UserLookupController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserSummary> lookup(@RequestParam(required = false) String keyword) {
        return userService.lookup(keyword);
    }
}
