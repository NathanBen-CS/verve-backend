package com.vervetutor.tutor_assistant.Document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonPdfRepository extends JpaRepository<LessonPDF, Long> {
}
