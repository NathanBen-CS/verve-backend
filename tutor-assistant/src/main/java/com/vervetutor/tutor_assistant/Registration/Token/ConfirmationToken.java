package com.vervetutor.tutor_assistant.Registration.Token;

import com.vervetutor.tutor_assistant.User.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class ConfirmationToken {
    public ConfirmationToken(User user, LocalDateTime createdAt, LocalDateTime expiresAt, String token) {
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.token = token;
        this.user = user;
    }

    @Id
    @SequenceGenerator(name = "confirmation_sequence", sequenceName = "confirmation_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "confirmation_sequence")
    private Long id;

    @Column(nullable = false)
    private String token;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime confirmedAt;

    @ManyToOne
    @JoinColumn(nullable = false, name="user_id")
    private User user;
}
