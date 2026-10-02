package com.vervetutor.tutor_assistant.Lesson;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long>{
    List<Lesson> findLessonByStudentId(Long id);
    List<Lesson> findLessonByTutorId(Long id);
    @Query("SELECT l FROM Lesson l WHERE l.dateTime >= :startOfDay AND l.dateTime <= :endOfDay AND l.status = 'Upcoming'")
    List<Lesson> findByDateTimeBetween(@Param("startOfDay") String startOfDay,
                                       @Param("endOfDay") String endOfDay);
}
