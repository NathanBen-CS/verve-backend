package com.vervetutor.tutor_assistant.Tutor;

import com.vervetutor.tutor_assistant.Student.Student;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TutorDto {
    private Long id;


    private String firstName;


    private String lastName;


    private List<Student> students = new ArrayList<>();


    private int aiQueries = 10;


    private int studentsLeft = 10;


    private int lessonsLeft = 20;
}
