package com.vervetutor.tutor_assistant.Lesson;

import com.vervetutor.tutor_assistant.Document.LessonPDF;
import com.vervetutor.tutor_assistant.Homework.Homework;
import com.vervetutor.tutor_assistant.Student.Student;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "lessons")
public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Storing the studentId as a foreign key
    @NotNull
    @Column(name = "student_id")
    private Long studentId;

    // Storing the tutorId as a foreign key
    @NotNull
    @Column(name = "tutor_id")
    private Long tutorId;

    @Column(name = "subject")
    private String subject;

    @Column(name = "grade_level")
    private String gradeLevel;

    @NotNull
    @Column(name = "date_time")
    private String dateTime;

    @Column(name = "duration")
    private int duration; // in minutes

    @NotNull
    @Column(name = "status")
    private String status; // Upcoming, Completed, Canceled

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private LessonFile pdf;

    @OneToOne
    @Nullable
    @JoinColumn(name = "homework_id")
    private Homework homework;

    // Helper methods to work with LocalDateTime
    public LocalDateTime getDateTimeAsLocalDateTime() {
        if (dateTime == null) return null;
        return LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public void setDateTimeFromLocalDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            this.dateTime = null;
        } else {
            this.dateTime = localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }
}
