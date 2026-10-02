package com.vervetutor.tutor_assistant.Student;

import com.vervetutor.tutor_assistant.Lesson.Lesson;
import com.vervetutor.tutor_assistant.Tutor.Tutor;
import jakarta.persistence.*;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)  // Ignore null values
public class StudentDto {
    private Long id;
    private String firstName;
    private String lastName;
    private Tutor tutor;
    private String email;
    private String[] notes;
    private String password;
    //private Lesson lesson;
}

