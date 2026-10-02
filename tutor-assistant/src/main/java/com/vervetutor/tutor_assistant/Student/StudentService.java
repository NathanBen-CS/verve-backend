package com.vervetutor.tutor_assistant.Student;

import com.vervetutor.tutor_assistant.Lesson.Lesson;
import com.vervetutor.tutor_assistant.Lesson.LessonService;
import com.vervetutor.tutor_assistant.Tutor.Tutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StudentService {
    @Autowired
    private final StudentRepository studentRepository;

    @Autowired
    private LessonService lessonService;

    public StudentService(StudentRepository studentRepository){
        this.studentRepository = studentRepository;
    }

    public Student createStudent(Student student) {
        return studentRepository.save(student);
    }

    public List<Student> findAllStudents() {
        return studentRepository.findAll();
    }

    public Optional<Student> findStudent(Long id) { return studentRepository.findById(id); }

    public Optional<Student> findStudent(String email) { return studentRepository.findByEmail(email); }

    public List<Student> findStudentByTutor(Long id) { return studentRepository.findByTutorId(id); }

    public void deleteStudent(Long id) {
        List<Lesson> lessons = lessonService.findLessonByStudent(id);
        for (Lesson lesson : lessons)
        {
            lessonService.deleteLesson(lesson.getId());
        }
        studentRepository.deleteById(id);
    }

    public boolean isExists(Long id) { return studentRepository.existsById(id); }

    public Student partialUpdate(Long id, Student student) {
        return studentRepository.findById(id).map(existingStudent -> {
            Optional.ofNullable(student.getFirstName()).ifPresent(existingStudent::setFirstName);
            Optional.ofNullable(student.getEmail()).ifPresent(existingStudent::setEmail);
            Optional.ofNullable(student.getLastName()).ifPresent(existingStudent::setLastName);
            Optional.ofNullable(student.getNotes()).ifPresent(existingStudent::setNotes);
            //Optional.ofNullable(student.getLessons()).ifPresent(existingStudent::setLessons);
            return studentRepository.save(existingStudent); // ✅ Save the updated existing student
        }).orElseThrow(() -> new RuntimeException("Student does not exist"));
    }

}