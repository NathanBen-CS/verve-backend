package com.vervetutor.tutor_assistant.Homework;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SubmissionDto {
    private Long id;
    private String fileName;
    private String contentType;
    private LocalDateTime createdAt;

    // Don't include the image bytes or homework object in the list response
    // The image will be fetched separately via the /image endpoint
}