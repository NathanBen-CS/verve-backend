package com.vervetutor.tutor_assistant.Ai;

import com.vervetutor.tutor_assistant.Config.JwtService;
import com.vervetutor.tutor_assistant.Tutor.Tutor;
import com.vervetutor.tutor_assistant.Tutor.TutorService;
import com.vervetutor.tutor_assistant.User.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController("/api")
public class AiContoller {

    private org.springframework.ai.chat.client.ChatClient chatClient;

    public AiContoller(ChatClient.Builder builder)
    {
        this.chatClient = builder.build();
    }
//
//    @GetMapping("/openAi")
//    public String createLesson(@RequestParam String prompt) {
//        return chatClient
//                .prompt()
//                .user(prompt)
//                .call().content();
//    }
}
