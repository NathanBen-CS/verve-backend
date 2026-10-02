package com.vervetutor.tutor_assistant.Lesson;

import com.vervetutor.tutor_assistant.Document.LessonPDF;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LessonDto {

    private Long id;

    // Storing the studentId as a foreign key in DTO format
    private Long studentId;

    // Storing the tutorId as a foreign key in DTO format
    private Long tutorId;

    private String subject;

    private String gradeLevel;

    private String dateTime;

    private int duration; // in minutes

    private String status; // Upcoming, Completed, Canceled
}
