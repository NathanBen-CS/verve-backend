package com.vervetutor.tutor_assistant.Email;

import com.vervetutor.tutor_assistant.Lesson.Lesson;
import com.vervetutor.tutor_assistant.Lesson.LessonRepository;
import com.vervetutor.tutor_assistant.User.User;
import com.vervetutor.tutor_assistant.User.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LessonReminderService {
    @Autowired
    LessonRepository lessonRepository;

    @Autowired
    UserService userService;

    @Autowired
    EmailService emailService;

    @Scheduled(cron = "0 0 7 * * ?") // 7:00 AM daily
    //@Scheduled(fixedRate = 30000)
    public void sendDailyLessonSummary() {
        // Get today's date range as strings
        String startOfDay = LocalDate.now().atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String endOfDay = LocalDate.now().atTime(23, 59, 59).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        List<Lesson> todaysLessons = lessonRepository.findByDateTimeBetween(startOfDay, endOfDay);

        Map<Long, List<Lesson>> lessonsByTutor = todaysLessons.stream()
                .collect(Collectors.groupingBy(Lesson::getTutorId));

        for (Long tutorId : lessonsByTutor.keySet()) {
            List<Lesson> tutorLessons = lessonsByTutor.get(tutorId);
            try {
                User tutor = userService.getUserByTutorId(tutorId);
                if (tutor != null && tutor.getEmail() != null) {
                    emailService.sendDailySummary(tutor.getEmail(), tutorLessons);
                }
            } catch (Exception e) {
                // Log error but continue processing other tutors
                System.err.println("Error sending daily summary to tutor " + tutorId + ": " + e.getMessage());
            }
        }
    }
}

