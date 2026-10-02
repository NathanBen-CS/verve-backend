package com.vervetutor.tutor_assistant.Registration;

import com.vervetutor.tutor_assistant.Tutor.Tutor;
import com.vervetutor.tutor_assistant.User.AppUserRole;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.*;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
@Setter
public class RegistrationRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private AppUserRole appUserRole = AppUserRole.USER;
}
