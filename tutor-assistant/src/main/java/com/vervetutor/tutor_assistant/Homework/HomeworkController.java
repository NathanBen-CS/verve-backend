package com.vervetutor.tutor_assistant.Homework;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vervetutor.tutor_assistant.Config.JwtService;
import com.vervetutor.tutor_assistant.Lesson.LessonFile;
import com.vervetutor.tutor_assistant.Mappers.HomeworkMapper;
import com.vervetutor.tutor_assistant.Student.Student;
import com.vervetutor.tutor_assistant.Student.StudentService;
import com.vervetutor.tutor_assistant.Tutor.Tutor;
import com.vervetutor.tutor_assistant.Tutor.TutorService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class HomeworkController {

    @Autowired
    private JwtService jwtService;
    @Autowired
    private TutorService tutorService;
    @Autowired
    private StudentService studentService;
    private final ChatClient chatClient;

    private final HomeworkMapper homeworkMapper;
    private final HomeworkService homeworkService;

    @Autowired
    public HomeworkController(HomeworkService homeworkService, HomeworkMapper homeworkMapper, ChatClient.Builder builder) {
        this.homeworkService = homeworkService;
        this.homeworkMapper = homeworkMapper;
        this.chatClient = builder.build();
    }

    @PostMapping("/homework/me")
    public ResponseEntity<HomeworkDto> createHomeworkForCurrentTutor(
            @RequestBody HomeworkDto homeworkDto) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userEmail = authentication.getName();
            Optional<Tutor> tutor = Optional.ofNullable(tutorService.findTutorByUserEmail(userEmail));

            if (tutor.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Assign this tutor to the homework (ignore anything from frontend)
            Homework homework = homeworkMapper.mapFrom(homeworkDto);
            homework.setTutorId(tutor.get().getId());

            Homework savedHomework = homeworkService.createHomework(homework);
            return new ResponseEntity<>(homeworkMapper.mapTo(savedHomework), HttpStatus.CREATED);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/homework/{id}")
    public ResponseEntity<HomeworkDto> getHomework(@PathVariable("id") Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userEmail = authentication.getName();
            Optional<Tutor> tutor = Optional.ofNullable(tutorService.findTutorByUserEmail(userEmail));
            Optional<Student> student = studentService.findStudent(userEmail);

            if (tutor.isEmpty() && student.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Optional<Homework> homeworkSaved = homeworkService.findHomework(id);
            if (homeworkSaved.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Homework homework = homeworkSaved.get();

            // Verify access: tutor owns it or student is assigned to it
            boolean hasAccess = false;
            if (tutor.isPresent() && homework.getTutorId().equals(tutor.get().getId())) {
                hasAccess = true;
            } else if (student.isPresent() && homework.getStudentId().equals(student.get().getId())) {
                hasAccess = true;
            }

            if (!hasAccess) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            return ResponseEntity.ok(homeworkMapper.mapTo(homework));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/homework/student/{id}/pdf")
    public ResponseEntity<byte[]> getStudentHomeworkPdf(@PathVariable("id") Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userEmail = authentication.getName();
            Student student = studentService.findStudent(userEmail)
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            Optional<Homework> homeworkOpt = homeworkService.findHomework(id);
            if (homeworkOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Homework homework = homeworkOpt.get();

            // Verify this homework is assigned to the current student
            if (!homework.getStudentId().equals(student.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            LessonFile file = homeworkService.getHomeworkFile(id);
            if (file == null || file.getData() == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"homework-" + id + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(file.getData());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/homework/tutor/me")
    public ResponseEntity<List<HomeworkDto>> findAllHomeworkByCurrentTutor() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userEmail = authentication.getName();
            Optional<Tutor> tutor = Optional.ofNullable(tutorService.findTutorByUserEmail(userEmail));

            if (tutor.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<Homework> homework = homeworkService.findHomeworksByTutor(tutor.get().getId());
            List<HomeworkDto> homeworkDtos = homework.stream()
                    .map(homeworkMapper::mapTo)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(homeworkDtos);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/homework/student/me")
    public ResponseEntity<List<HomeworkDto>> findAllHomeworkByCurrentStudent() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userEmail = authentication.getName();
            Student student = studentService.findStudent(userEmail)
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            List<Homework> homework = homeworkService.findHomeworksByStudent(student.getId());
            List<HomeworkDto> homeworkDtos = homework.stream()
                    .map(homeworkMapper::mapTo)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(homeworkDtos);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/homework/student/{id}")
    public ResponseEntity<List<HomeworkDto>> findAllHomeworkByStudentForCurrentTutor(
            @PathVariable("id") Long studentId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userEmail = authentication.getName();
            Optional<Tutor> tutor = Optional.ofNullable(tutorService.findTutorByUserEmail(userEmail));

            if (tutor.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Ensure this student belongs to this tutor
            boolean ownsStudent = tutor.get().getStudents()
                    .stream()
                    .anyMatch(student -> student.getId().equals(studentId));

            if (!ownsStudent) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<Homework> homework = homeworkService.findHomeworksByStudent(studentId);
            List<HomeworkDto> homeworkDtos = homework.stream()
                    .map(homeworkMapper::mapTo)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(homeworkDtos);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/homework/{id}")
    public ResponseEntity<?> deleteHomework(@PathVariable("id") Long homeworkId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userEmail = authentication.getName();
            Optional<Tutor> tutor = Optional.ofNullable(tutorService.findTutorByUserEmail(userEmail));

            if (tutor.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Optional<Homework> homeworkOpt = homeworkService.findHomework(homeworkId);

            if (homeworkOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Homework homework = homeworkOpt.get();

            // Ensure the tutor owns this homework
            if (!homework.getTutorId().equals(tutor.get().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            homeworkService.deleteHomework(homeworkId);
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete homework"));
        }
    }

    @PatchMapping(path = "/homework/{id}")
    public ResponseEntity<?> partialUpdate(
            @PathVariable Long id,
            @RequestBody HomeworkDto homeworkDto) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }

            String userEmail = authentication.getName();
            Optional<Tutor> tutorOpt = Optional.ofNullable(tutorService.findTutorByUserEmail(userEmail));
            if (tutorOpt.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }

            Optional<Homework> existingHomeworkOpt = homeworkService.findHomework(id);
            if (existingHomeworkOpt.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            Homework existingHomework = existingHomeworkOpt.get();
            if (!existingHomework.getTutorId().equals(tutorOpt.get().getId())) {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }

            Homework homeworkToUpdate = homeworkMapper.mapFrom(homeworkDto);
            Homework updatedHomework = homeworkService.partialUpdate(id, homeworkToUpdate);

            return new ResponseEntity<>(homeworkMapper.mapTo(updatedHomework), HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update homework");
        }
    }

    @GetMapping("/homework/{id}/pdf")
    public ResponseEntity<byte[]> getHomeworkPdf(@PathVariable("id") Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userEmail = authentication.getName();
            Tutor tutor = tutorService.findTutorByUserEmail(userEmail);
            if (tutor == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            LessonFile file = homeworkService.getHomeworkFile(id);
            if (file == null || file.getData() == null) {
                return ResponseEntity.notFound().build();
            }

            Optional<Homework> homeworkOpt = homeworkService.findHomework(id);
            if (homeworkOpt.isEmpty() || !homeworkOpt.get().getTutorId().equals(tutor.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"homework-" + id + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(file.getData());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/homework/{id}/pdf")
    public ResponseEntity<?> createHomeworkPdf(
            @PathVariable("id") Long id,
            @RequestParam("data") MultipartFile file) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userEmail = authentication.getName();
            Tutor tutor = tutorService.findTutorByUserEmail(userEmail);
            if (tutor == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Optional<Homework> homeworkOpt = homeworkService.findHomework(id);
            if (homeworkOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Homework homework = homeworkOpt.get();
            if (!homework.getTutorId().equals(tutor.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            byte[] fileBytes = file.getBytes();
            String fileName = file.getOriginalFilename();
            String contentType = file.getContentType();

            homeworkService.addHomeworkFile(id, fileBytes, fileName, contentType);

            return ResponseEntity.ok().build();

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to process file");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/homework/submissions/{submissionId}/image")
    public ResponseEntity<byte[]> getSubmissionImageForTutor(
            @PathVariable("submissionId") Long submissionId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userEmail = authentication.getName();
            Tutor tutor = tutorService.findTutorByUserEmail(userEmail);

            // Get submission and verify it belongs to the student
            Optional<Submission> submissionOpt = homeworkService.findSubmission(submissionId);

            if (submissionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Submission submission = submissionOpt.get();
            Homework homework = submission.getHomework();

            if (!homework.getTutorId().equals(tutor.getId()))
            {
                return ResponseEntity.notFound().build();
            }

            // Get the image data using the service method
            byte[] imageData = homeworkService.getSubmissionImage(submissionId);

            // Return the image bytes with correct headers
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + submission.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(submission.getContentType()))
                    .body(imageData);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/homework/{id}/grade")
    public ResponseEntity<?> gradeHomework(@PathVariable("id") Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userEmail = authentication.getName();
            Tutor tutor = tutorService.findTutorByUserEmail(userEmail);
            if (tutor == null) {
                throw new RuntimeException("Tutor not found");
            }

            Homework homework = homeworkService.findHomework(id)
                    .orElseThrow(() -> new RuntimeException("Homework not found"));

            if (!homework.getTutorId().equals(tutor.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            if (tutor.getAiQueries() <= 0) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("error", "No AI queries left"));
            }

            // Fetch all submissions for this homework
            List<Submission> submissions = homeworkService.findSubmissionsByHomework(id);
            if (submissions == null || submissions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No submissions found for grading"));
            }

            // Filter only valid image submissions
            List<Submission> validSubmissions = submissions.stream()
                    .filter(sub -> sub.getImage() != null && sub.getImage().length > 0)
                    .collect(Collectors.toList());

            if (validSubmissions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No valid images found in submissions"));
            }

            // Convert to Spring AI Media objects
            List<org.springframework.ai.model.Media> mediaList = validSubmissions.stream().map(sub -> {
                ByteArrayResource resource = new ByteArrayResource(sub.getImage()) {
                    @Override
                    public String getFilename() {
                        return sub.getFileName() != null ? sub.getFileName() : "submission_" + sub.getId() + ".jpg";
                    }
                };
                String mimeType = determineMimeType(sub.getContentType(), sub.getFileName());
                return new org.springframework.ai.model.Media(org.springframework.util.MimeType.valueOf(mimeType), resource);
            }).collect(Collectors.toList());

            if (mediaList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No processable images found"));
            }

            // Deduct 1 AI query
            tutor.setAiQueries(tutor.getAiQueries() - 1);
            tutorService.saveTutor(tutor);

            String prompt = """
            You are a professional homework grading assistant. Analyze the provided homework images and provide:
            1. A detailed feedback on the student's work
            2. A numeric score between 0 and 100

            If the image is blurry, unreadable, or unclear, respond ONLY with the text: "Photo not Clear Enough".

            Otherwise, respond in *this exact JSON format*:
            {
              "score": <number between 0-100>,
              "feedback": "<detailed feedback here>"
            }
        """;

            String aiResponse;
            try {
                aiResponse = chatClient.prompt()
                        .user(userSpec -> userSpec.text(prompt).media(mediaList.toArray(new org.springframework.ai.model.Media[0])))
                        .call()
                        .content();
            } catch (Exception e) {
                tutor.setAiQueries(tutor.getAiQueries() + 1); // refund
                tutorService.saveTutor(tutor);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "AI processing failed: " + e.getMessage()));
            }

            if (aiResponse == null || aiResponse.trim().equalsIgnoreCase("Photo not Clear Enough")) {
                tutor.setAiQueries(tutor.getAiQueries() + 1);
                tutorService.saveTutor(tutor);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Photo not Clear Enough"));
            }

            // Try parsing JSON
            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode json = mapper.readTree(aiResponse);

                if (!json.has("score") || !json.has("feedback")) {
                    throw new RuntimeException("Missing fields");
                }

                int score = json.get("score").asInt();
                if (score < 0 || score > 100) throw new RuntimeException("Invalid score range");

                // ✅ Update score, status, and feedback
                homework.setScore(String.valueOf(score));
                homework.setFeedback(json.get("feedback").asText()); // <-- NEW LINE
                homework.setStatus("GRADED");
                homeworkService.partialUpdate(homework.getId(), homework);

                Map<String, Object> response = Map.of(
                        "homeworkId", homework.getId(),
                        "score", score,
                        "feedback", json.get("feedback").asText(),
                        "message", "Homework graded successfully"
                );

                return ResponseEntity.ok(response);
            } catch (Exception e) {
                tutor.setAiQueries(tutor.getAiQueries() + 1); // refund
                tutorService.saveTutor(tutor);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Invalid AI JSON: " + e.getMessage(), "rawResponse", aiResponse));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to grade homework: " + e.getMessage()));
        }
    }

    // Helper method
    private String determineMimeType(String contentType, String fileName) {
        if (contentType != null && !contentType.isBlank()) return contentType;
        if (fileName != null) {
            String f = fileName.toLowerCase();
            if (f.endsWith(".png")) return "image/png";
            if (f.endsWith(".jpg") || f.endsWith(".jpeg")) return "image/jpeg";
            if (f.endsWith(".gif")) return "image/gif";
            if (f.endsWith(".webp")) return "image/webp";
        }
        return "image/jpeg";
    }



    @PostMapping("/homework/{id}/submissions")
    public ResponseEntity<?> createHomeworkSubmissions(
            @PathVariable("id") Long id,
            @RequestParam("images") List<MultipartFile> images) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userEmail = authentication.getName();
            Student student = studentService.findStudent(userEmail)
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            Optional<Homework> homeworkOpt = homeworkService.findHomework(id);
            if (homeworkOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Homework homework = homeworkOpt.get();
            if (!homework.getStudentId().equals(student.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            if (images == null || images.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No images provided"));
            }

            for (MultipartFile image : images) {
                if (image.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "One or more images are empty"));
                }
                String contentType = image.getContentType();
                if (contentType == null ||
                        (!contentType.equals("image/jpeg") &&
                                !contentType.equals("image/png") &&
                                !contentType.equals("image/jpg"))) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Only JPEG, JPG, and PNG images are allowed"));
                }
            }

            // Save each submission
            for (MultipartFile image : images) {
                homeworkService.createSubmissionWithImage(id, image);
            }

            return ResponseEntity.ok(Map.of("message", "Submissions uploaded successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload submissions: " + e.getMessage()));
        }
    }

    @GetMapping("/homework/{id}/submissions")
    public ResponseEntity<List<SubmissionDto>> getHomeworkSubmissions(
            @PathVariable("id") Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userEmail = authentication.getName();
            Optional<Tutor> tutor = Optional.ofNullable(tutorService.findTutorByUserEmail(userEmail));
            Optional<Student> student = studentService.findStudent(userEmail);

            Optional<Homework> homeworkOpt = homeworkService.findHomework(id);
            if (homeworkOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Homework homework = homeworkOpt.get();

            boolean hasAccess = false;
            if (tutor.isPresent() && homework.getTutorId().equals(tutor.get().getId())) {
                hasAccess = true;
            } else if (student.isPresent() && homework.getStudentId().equals(student.get().getId())) {
                hasAccess = true;
            }

            if (!hasAccess) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<SubmissionDto> submissions = homeworkService.findSubmissionsByHomeworkAsDto(id);
            return ResponseEntity.ok(submissions);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Student specific endpoints
    @GetMapping("/homework/student/they")
    public ResponseEntity<List<HomeworkDto>> findAllHomeworkByCurrentStudent2() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userEmail = authentication.getName();
            Student student = studentService.findStudent(userEmail)
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            List<Homework> homework = homeworkService.findHomeworksByStudent(student.getId());
            List<HomeworkDto> homeworkDtos = homework.stream()
                    .map(homeworkMapper::mapTo)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(homeworkDtos);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/homework/student/they/{homeworkId}/submissions")
    public ResponseEntity<List<SubmissionDto>> getHomeworkSubmissionsForStudent(
            @PathVariable("homeworkId") Long homeworkId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userEmail = authentication.getName();
            Student student = studentService.findStudent(userEmail)
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            // Verify the homework belongs to this student
            Optional<Homework> homeworkOpt = homeworkService.findHomework(homeworkId);
            if (homeworkOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Homework homework = homeworkOpt.get();
            if (!homework.getStudentId().equals(student.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<SubmissionDto> submissions = homeworkService.findSubmissionsByHomeworkAsDto(homeworkId);
            return ResponseEntity.ok(submissions);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/homework/student/they/{homeworkId}/submissions")
    public ResponseEntity<?> createHomeworkSubmissionForStudent(
            @PathVariable("homeworkId") Long homeworkId,
            @RequestParam("images") List<MultipartFile> images) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userEmail = authentication.getName();
            Student student = studentService.findStudent(userEmail)
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            // Verify homework exists and belongs to this student
            Optional<Homework> homeworkOpt = homeworkService.findHomework(homeworkId);
            if (homeworkOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Homework homework = homeworkOpt.get();
            if (!homework.getStudentId().equals(student.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Validate images
            if (images == null || images.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No images provided"));
            }

            // Validate file types and sizes
            for (MultipartFile image : images) {
                if (image.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "One or more images are empty"));
                }
                String contentType = image.getContentType();
                if (contentType == null ||
                        (!contentType.equals("image/jpeg") &&
                                !contentType.equals("image/png") &&
                                !contentType.equals("image/jpg"))) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Only JPEG, JPG, and PNG images are allowed"));
                }
            }

            // Save each submission
            for (MultipartFile image : images) {
                homeworkService.createSubmissionWithImage(homeworkId, image);
            }

            // Update homework status to "In Progress" if it's the first submission
            if ("Assigned".equals(homework.getStatus())) {
                homework.setStatus("In Progress");
                homeworkService.partialUpdate(homeworkId, homework);
            }

            return ResponseEntity.ok(Map.of("message", "Homework submitted successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to submit homework: " + e.getMessage()));
        }
    }

    @GetMapping("/homework/student/they/submissions/{submissionId}/image")
    public ResponseEntity<byte[]> getSubmissionImageForStudent(
            @PathVariable("submissionId") Long submissionId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userEmail = authentication.getName();
            Student student = studentService.findStudent(userEmail)
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            // Get submission and verify it belongs to the student
            Optional<Submission> submissionOpt = homeworkService.findSubmission(submissionId);
            if (submissionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Submission submission = submissionOpt.get();
            Homework homework = submission.getHomework();

            if (!homework.getStudentId().equals(student.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Get the image data using the service method
            byte[] imageData = homeworkService.getSubmissionImage(submissionId);

            // Return the image bytes with correct headers
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + submission.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(submission.getContentType()))
                    .body(imageData);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}