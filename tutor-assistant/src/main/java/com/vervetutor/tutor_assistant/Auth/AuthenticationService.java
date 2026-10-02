package com.vervetutor.tutor_assistant.Auth;

import com.vervetutor.tutor_assistant.Config.JwtService;
import com.vervetutor.tutor_assistant.Config.PasswordEncoder;
import com.vervetutor.tutor_assistant.Student.StudentRepository;
import com.vervetutor.tutor_assistant.User.AppUserRole;
import com.vervetutor.tutor_assistant.User.User;
import com.vervetutor.tutor_assistant.User.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final StudentRepository studentRepository;

    public AuthenticationResponse register(RegisterRequest request) {
        var user = User.builder().firstName(request.getFirstName()).lastName(request.getLastName()).email(request.getEmail())
                       .password(bCryptPasswordEncoder.encode(request.getPassword())).appUserRole(AppUserRole.USER).build();

        if (userRepository.findByEmail(request.getEmail()).orElseThrow().getEnabled()){
            var jwtToken = jwtService.generateToken(user);
            return AuthenticationResponse.builder().token(jwtToken).build();
        }
        return null;
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        var user = userRepository.findByEmail(request.getEmail()).orElseThrow();

        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder().token(jwtToken).build();
    }
}
