package com.vervetutor.tutor_assistant.Document;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name="pdfs")
public class LessonPDF {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "lesson_id")
    private Long lessonId;
    
    @NotNull
    private String fileName;

    @Lob
    private byte[] content;
}
