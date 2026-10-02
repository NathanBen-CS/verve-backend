package com.vervetutor.tutor_assistant.Homework;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HomeworkDto {
    private Long id;
    private Long studentId;
    private Long tutorId;
    private String subject;
    private String description;
    private String score;
    private String dueDate;
    private String status;
    private String feedback;
}