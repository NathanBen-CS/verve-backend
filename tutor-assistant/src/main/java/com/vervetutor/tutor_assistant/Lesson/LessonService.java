package com.vervetutor.tutor_assistant.Lesson;

import com.vervetutor.tutor_assistant.Document.LessonPDF;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class LessonService {
    private LessonRepository lessonRepository;

    public LessonService(LessonRepository lessonRepository) {
        this.lessonRepository = lessonRepository;
    }

    public void createLesson(Lesson lesson) {
        lessonRepository.save(lesson);
    }

    public List<Lesson> findAllLessons() {
        return lessonRepository.findAll();
    }

    public Optional<Lesson> findLesson(Long id) {
        return lessonRepository.findById(id);
    }

    public List<Lesson> findLessonByTutor(Long id) {
        return lessonRepository.findLessonByTutorId(id);
    }

    public boolean isExists(Long id) {
        return lessonRepository.existsById(id);
    }

    public Lesson partialUpdate(Long id, Lesson lesson) {
        return lessonRepository.findById(id).map(existingLesson -> {
            // Update fields if they are not null in the provided lesson object
            Optional.ofNullable(lesson.getStudentId()).ifPresent(existingLesson::setStudentId);
            Optional.ofNullable(lesson.getTutorId()).ifPresent(existingLesson::setTutorId);
            Optional.ofNullable(lesson.getSubject()).ifPresent(existingLesson::setSubject);
            Optional.ofNullable(lesson.getGradeLevel()).ifPresent(existingLesson::setGradeLevel);
            Optional.ofNullable(lesson.getDateTime()).ifPresent(existingLesson::setDateTime);
            Optional.ofNullable(lesson.getDuration()).ifPresent(existingLesson::setDuration);
            Optional.ofNullable(lesson.getStatus()).ifPresent(existingLesson::setStatus);

            // Save the updated existing lesson
            return lessonRepository.save(existingLesson);
        }).orElseThrow(() -> new RuntimeException("Lesson with id " + id + " does not exist"));
    }

    public List<Lesson> findLessonByStudent(Long id) {
        return lessonRepository.findLessonByStudentId(id);
    }

    public void deleteLesson(Long id) {
        lessonRepository.deleteById(id);
    }

    public void addFile(Lesson lesson, byte[] data) {
        // Create a LessonFile object, set its data, and associate it with the lesson
        LessonFile lessonFile = new LessonFile();
        lessonFile.setData(data);

        // Save the LessonFile object
        lesson.setPdf(lessonFile);  // Link the LessonFile to the Lesson
        lessonRepository.save(lesson);  // Save the Lesson (which also saves the LessonFile)
    }

    @Transactional(readOnly = true)
    public LessonFile getFile(Long id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Lesson not found with id: " + id));

        Hibernate.initialize(lesson.getPdf());
        LessonFile file = lesson.getPdf();

        if (file == null) {
            throw new IllegalStateException("No PDF file found for lesson with id: " + id);
        }

        // This forces Hibernate to fetch the LOB data before session closes
        Hibernate.initialize(file.getData());

        return file;
    }
}
