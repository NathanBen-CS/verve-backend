package com.vervetutor.tutor_assistant.Tutor;

import com.vervetutor.tutor_assistant.User.User;
import com.vervetutor.tutor_assistant.User.UserService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
public class TutorService {
    @Autowired
    private TutorRepository tutorRepository;

    @Autowired
    private UserService userService;

    public TutorService(TutorRepository tutorRepository) {
        this.tutorRepository = tutorRepository;
    }

    public Tutor findTutorByUserEmail(String email) {
        return userService.findByEmail(email)
                .map(User::getTutor)
                .orElseThrow(() -> new RuntimeException("No tutor found for user with email: " + email));
    }

    //fix magic values when integrating with stripe plans
    public Tutor createTutor(Tutor tutor) {
        if (tutor.getAiQueries() == 0) tutor.setAiQueries(10);
        if (tutor.getStudentsLeft() == 0) tutor.setStudentsLeft(10);
        if (tutor.getLessonsLeft() == 0) tutor.setLessonsLeft(20);
        return tutorRepository.save(tutor);
    }

    public Tutor saveTutor(Tutor tutor) {
        return tutorRepository.save(tutor);
    }

    public List<Tutor> findAllTutors() {
        return tutorRepository.findAll();
    }

    public Optional<Tutor> findTutor(Long id) {
        return tutorRepository.findById(id);
    }
}
