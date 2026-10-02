package com.vervetutor.tutor_assistant.Student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findByTutorId(Long tutorId);
    Optional<Student> findByEmail(String email);
}