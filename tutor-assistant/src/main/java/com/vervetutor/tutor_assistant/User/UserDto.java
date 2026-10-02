package com.vervetutor.tutor_assistant.User;

import com.vervetutor.tutor_assistant.Tutor.Tutor;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String phone;
    private String profilePhoto;
    private String address;
    private Tutor tutor;
    private AppUserRole appUserRole;
    private Boolean locked;
    private Boolean enabled;
    private Boolean active;
    private Boolean notifications;
    private String stripeId;
}