package com.vervetutor.tutor_assistant.Tutor;

import com.vervetutor.tutor_assistant.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface TutorRepository extends JpaRepository<Tutor, Long> {
}