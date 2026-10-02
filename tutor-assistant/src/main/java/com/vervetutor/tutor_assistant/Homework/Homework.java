package com.vervetutor.tutor_assistant.Homework;

import com.vervetutor.tutor_assistant.Lesson.LessonFile;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "homework")
public class Homework {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "student_id")
    private Long studentId;

    @NotNull
    @Column(name = "tutor_id")
    private Long tutorId;

    @Column(name = "subject")
    private String subject;

    @Column(name = "description")
    private String description;

    @Column(name = "score")
    private String score;

    @Column(name = "due_date")
    private String dueDate;

    @Column(name = "status")
    private String status;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private LessonFile pdf;

    @Column(name = "feedback", length = 2000)
    private String feedback;

    @OneToMany(mappedBy = "homework", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Submission> submissions = new ArrayList<>();

    // Properly implemented addSubmission method
    public void addSubmission(Submission submission) {
        if (submission != null) {
            if (this.submissions == null) {
                this.submissions = new ArrayList<>();
            }
            this.submissions.add(submission);
            submission.setHomework(this); // Set the bidirectional relationship
        }
    }
}