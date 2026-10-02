package com.vervetutor.tutor_assistant.User;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Request class for changing user password
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordChangeRequest {
    private String currentPassword;
    private String newPassword;
}