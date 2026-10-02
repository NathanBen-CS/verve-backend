package com.vervetutor.tutor_assistant.Student;

import com.vervetutor.tutor_assistant.Lesson.Lesson;
import com.vervetutor.tutor_assistant.Tutor.Tutor;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "students")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @ManyToOne
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    @Column(name = "email")
    private String email;

    @Column(name = "notes")
    private String[] notes;

    //Add notes

    //@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    //private List<Lesson> lessons = new ArrayList<>();

    /*@OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<String> homework = new ArrayList<>();*/
}
