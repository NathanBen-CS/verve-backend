package com.vervetutor.tutor_assistant.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request class for updating user profile information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private Boolean notifications;
}