package com.vervetutor.tutor_assistant.Homework;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HomeworkRepository extends JpaRepository<Homework, Long> {
    List<Homework> findByTutorId(Long tutorId);
    List<Homework> findByStudentId(Long studentId);
    List<Homework> findByStudentIdAndTutorId(Long studentId, Long tutorId);
    List<Homework> findByStatus(String status);
}