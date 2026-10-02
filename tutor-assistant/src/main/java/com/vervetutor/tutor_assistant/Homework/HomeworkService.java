package com.vervetutor.tutor_assistant.Homework;

import com.vervetutor.tutor_assistant.Lesson.LessonFile;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class HomeworkService {
    private final HomeworkRepository homeworkRepository;
    private final SubmissionRepository submissionRepository;

    public HomeworkService(HomeworkRepository homeworkRepository, SubmissionRepository submissionRepository) {
        this.homeworkRepository = homeworkRepository;
        this.submissionRepository = submissionRepository;
    }

    // Homework CRUD operations
    @Transactional
    public Homework createHomework(Homework homework) {
        return homeworkRepository.save(homework);
    }

    public List<Homework> findAllHomeworks() {
        return homeworkRepository.findAll();
    }

    public Optional<Homework> findHomework(Long id) {
        return homeworkRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Homework findHomeworkWithSubmissions(Long id) {
        Homework homework = homeworkRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Homework not found with id: " + id));

        // Force initialization of lazy collection
        Hibernate.initialize(homework.getSubmissions());

        return homework;
    }

    public List<Homework> findHomeworksByTutor(Long tutorId) {
        return homeworkRepository.findByTutorId(tutorId);
    }

    public List<Homework> findHomeworksByStudent(Long studentId) {
        return homeworkRepository.findByStudentId(studentId);
    }

    public List<Homework> findHomeworksByStudentAndTutor(Long studentId, Long tutorId) {
        return homeworkRepository.findByStudentIdAndTutorId(studentId, tutorId);
    }

    public List<Homework> findHomeworksByStatus(String status) {
        return homeworkRepository.findByStatus(status);
    }

    public boolean homeworkExists(Long id) {
        return homeworkRepository.existsById(id);
    }

    @Transactional
    public Homework partialUpdate(Long id, Homework homeworkUpdates) {
        return homeworkRepository.findById(id).map(existingHomework -> {
            // Update fields if they are not null in the provided homework object
            Optional.ofNullable(homeworkUpdates.getStudentId()).ifPresent(existingHomework::setStudentId);
            Optional.ofNullable(homeworkUpdates.getTutorId()).ifPresent(existingHomework::setTutorId);
            Optional.ofNullable(homeworkUpdates.getSubject()).ifPresent(existingHomework::setSubject);
            Optional.ofNullable(homeworkUpdates.getDescription()).ifPresent(existingHomework::setDescription);
            Optional.ofNullable(homeworkUpdates.getScore()).ifPresent(existingHomework::setScore);
            Optional.ofNullable(homeworkUpdates.getDueDate()).ifPresent(existingHomework::setDueDate);
            Optional.ofNullable(homeworkUpdates.getStatus()).ifPresent(existingHomework::setStatus);
            Optional.ofNullable(homeworkUpdates.getPdf()).ifPresent(existingHomework::setPdf);

            return homeworkRepository.save(existingHomework);
        }).orElseThrow(() -> new RuntimeException("Homework with id " + id + " does not exist"));
    }

    @Transactional
    public void deleteHomework(Long id) {
        homeworkRepository.deleteById(id);
    }

    // Submission operations
    @Transactional
    public Submission addSubmission(Long homeworkId, Submission submission) {
        Homework homework = homeworkRepository.findById(homeworkId)
                .orElseThrow(() -> new IllegalStateException("Homework not found with id: " + homeworkId));

        homework.addSubmission(submission);
        homeworkRepository.save(homework);

        return submissionRepository.save(submission);
    }

    @Transactional
    public Submission createSubmission(Submission submission) {
        return submissionRepository.save(submission);
    }

    public Optional<Submission> findSubmission(Long id) {
        return submissionRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Submission> findSubmissionsByHomework(Long homeworkId) {
        return submissionRepository.findByHomeworkId(homeworkId);
    }

    @Transactional(readOnly = true)
    public List<SubmissionDto> findSubmissionsByHomeworkAsDto(Long homeworkId) {
        List<Submission> submissions = submissionRepository.findByHomeworkId(homeworkId);
        return submissions.stream()
                .map(this::convertToSubmissionDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Submission> findSubmissionsByStudent(Long studentId) {
        // First get all homeworks for the student, then get their submissions
        List<Homework> studentHomeworks = homeworkRepository.findByStudentId(studentId);
        return studentHomeworks.stream()
                .flatMap(homework -> homework.getSubmissions().stream())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubmissionDto> findSubmissionsByStudentAsDto(Long studentId) {
        List<Homework> studentHomeworks = homeworkRepository.findByStudentId(studentId);
        return studentHomeworks.stream()
                .flatMap(homework -> homework.getSubmissions().stream())
                .map(this::convertToSubmissionDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Submission updateSubmissionScore(Long submissionId, String score) {
        return submissionRepository.findById(submissionId).map(submission -> {
            // You might want to update the homework status as well when a score is added
            Homework homework = submission.getHomework();
            if (homework != null) {
                homework.setScore(score);
                homework.setStatus("GRADED");
                homeworkRepository.save(homework);
            }
            // If you want to store score on submission as well, you can add a score field to Submission
            return submission;
        }).orElseThrow(() -> new RuntimeException("Submission with id " + submissionId + " does not exist"));
    }

    @Transactional
    public void deleteSubmission(Long id) {
        submissionRepository.deleteById(id);
    }

    // File operations
    @Transactional
    public void addHomeworkFile(Long homeworkId, byte[] data, String fileName, String contentType) {
        Homework homework = homeworkRepository.findById(homeworkId)
                .orElseThrow(() -> new IllegalStateException("Homework not found with id: " + homeworkId));

        LessonFile lessonFile = new LessonFile();
        lessonFile.setData(data);

        homework.setPdf(lessonFile);
        homeworkRepository.save(homework);
    }

    @Transactional
    public void addSubmissionFile(Submission submission) {
        submissionRepository.save(submission);
    }

    @Transactional(readOnly = true)
    public LessonFile getHomeworkFile(Long homeworkId) {
        Homework homework = homeworkRepository.findById(homeworkId)
                .orElseThrow(() -> new IllegalStateException("Homework not found with id: " + homeworkId));

        Hibernate.initialize(homework.getPdf());
        LessonFile file = homework.getPdf();

        if (file == null) {
            throw new IllegalStateException("No PDF file found for homework with id: " + homeworkId);
        }

        Hibernate.initialize(file.getData());
        return file;
    }

    @Transactional(readOnly = true)
    public byte[] getSubmissionImage(Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalStateException("Submission not found with id: " + submissionId));

        if (submission.getImage() == null || submission.getImage().length == 0) {
            throw new IllegalStateException("No image found for submission with id: " + submissionId);
        }

        return submission.getImage();
    }

    // Business logic methods
    @Transactional
    public Homework updateHomeworkStatus(Long homeworkId, String status) {
        return homeworkRepository.findById(homeworkId).map(homework -> {
            homework.setStatus(status);
            return homeworkRepository.save(homework);
        }).orElseThrow(() -> new RuntimeException("Homework with id " + homeworkId + " does not exist"));
    }

    public boolean hasStudentSubmittedHomework(Long homeworkId, Long studentId) {
        Homework homework = homeworkRepository.findById(homeworkId)
                .orElseThrow(() -> new IllegalStateException("Homework not found with id: " + homeworkId));

        // Verify this homework belongs to the student
        if (!homework.getStudentId().equals(studentId)) {
            throw new IllegalStateException("Homework does not belong to student with id: " + studentId);
        }

        return !homework.getSubmissions().isEmpty();
    }

    public int getSubmissionCount(Long homeworkId) {
        Homework homework = homeworkRepository.findById(homeworkId)
                .orElseThrow(() -> new IllegalStateException("Homework not found with id: " + homeworkId));

        return homework.getSubmissions().size();
    }

    public Submission getSubmission(Long submissionId) {
        return submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalStateException("Submission not found with id: " + submissionId));
    }

    @Transactional
    public void addSubmissions(Homework homework, List<MultipartFile> images) throws IOException {
        for (MultipartFile image : images) {
            Submission submission = Submission.builder()
                    .fileName(image.getOriginalFilename())
                    .contentType(image.getContentType())
                    .build();

            Submission savedSubmission = addSubmission(homework.getId(), submission);
        }
    }

    @Transactional
    public Submission createSubmissionWithImage(Long homeworkId, MultipartFile image) throws IOException {
        Homework homework = homeworkRepository.findById(homeworkId)
                .orElseThrow(() -> new IllegalStateException("Homework not found with id: " + homeworkId));

        Submission submission = Submission.builder()
                .fileName(image.getOriginalFilename())
                .contentType(image.getContentType())
                .image(image.getBytes())
                .homework(homework)
                .build();

        return submissionRepository.save(submission);
    }

    // DTO conversion methods
    private SubmissionDto convertToSubmissionDto(Submission submission) {
        if (submission == null) {
            return null;
        }

        SubmissionDto dto = new SubmissionDto();
        dto.setId(submission.getId());
        dto.setFileName(submission.getFileName());
        dto.setContentType(submission.getContentType());
        dto.setCreatedAt(submission.getCreatedAt());
        return dto;
    }
}