package com.vervetutor.tutor_assistant.Registration;

import com.vervetutor.tutor_assistant.User.AppUserRole;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(path = "registration")
@AllArgsConstructor
public class RegistrationContoller {
    @Autowired
    private RegistrationService registrationService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegistrationRequest request) {
        Map<String, String> response = new HashMap<>();
        request.setAppUserRole(AppUserRole.USER);
        response.put("message", registrationService.register(request));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/confirm")
    public String confirm(@RequestParam("token") String token)
    {
        return registrationService.confirmToken(token);
    }
}
