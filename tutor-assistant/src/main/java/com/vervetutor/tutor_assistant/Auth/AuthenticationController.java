package com.vervetutor.tutor_assistant.Auth;

import com.vervetutor.tutor_assistant.Config.JwtService;
import com.vervetutor.tutor_assistant.User.AppUserRole;
import com.vervetutor.tutor_assistant.User.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "https://www.vervetutor.com", allowCredentials = "true")
public class AuthenticationController {

    private final AuthenticationService service;
    private final JwtService jwtService;
    private final UserService userDetailsService;

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> register (@RequestBody AuthenticationRequest request)
    {
        return ResponseEntity.ok(service.authenticate(request));
    }

//    @PostMapping("/authenticate/student")
//    public ResponseEntity<AuthenticationResponse> loginStudent (@RequestBody AuthenticationRequest request)
//    {
//        return ResponseEntity.ok(service.authenticate(request));
//    }

    @GetMapping("/ping")
    public ResponseEntity<String> register ()
    {
        return ResponseEntity.ok("pong");
    }

    @GetMapping("/validate")
    public ResponseEntity<Integer> validateToken(HttpServletRequest request) {
        try {
            final String authHeader = request.getHeader("Authorization");
            final String jwt;
            final String userEmail;

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.ok(-1);
            }

            jwt = authHeader.substring(7);
            userEmail = jwtService.extractUsername(jwt);

            if (userEmail == null) {
                return ResponseEntity.ok(-1);
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
            boolean isValid = jwtService.isTokenValid(jwt, userDetails);
            if (isValid){
                AppUserRole role = userDetailsService.findByEmail(userEmail).orElseThrow().getAppUserRole();
                return ResponseEntity.ok(role == AppUserRole.USER ? 1 : 2);
            }
            else{
                return ResponseEntity.ok(-1);
            }

        } catch (Exception e) {
            return ResponseEntity.ok(-1);
        }
    }
}
