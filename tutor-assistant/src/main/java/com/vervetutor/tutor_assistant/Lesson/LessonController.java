package com.vervetutor.tutor_assistant.Lesson;

import com.vervetutor.tutor_assistant.Config.JwtService;
import com.vervetutor.tutor_assistant.Mappers.LessonMapper;
import com.vervetutor.tutor_assistant.Student.Student;
import com.vervetutor.tutor_assistant.Student.StudentService;
import com.vervetutor.tutor_assistant.Tutor.Tutor;
import com.vervetutor.tutor_assistant.Tutor.TutorService;
import io.netty.handler.timeout.TimeoutException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
public class LessonController {

    @Autowired
    private JwtService jwtService;
    @Autowired
    private TutorService tutorService;
    private final LessonService lessonService;
    private final LessonMapper lessonMapper;
    private final ChatClient chatClient;
    @Autowired
    private StudentService studentService;

    @Autowired
    public LessonController(LessonService lessonService, LessonMapper lessonMapper, ChatClient.Builder builder) {
        this.lessonService = lessonService;
        this.lessonMapper = lessonMapper;
        this.chatClient = builder.build();
    }

    @PostMapping("/lessons/me")
    public ResponseEntity<LessonDto> createLessonForCurrentTutor(
            @RequestBody LessonDto lessonDto) {
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

            // Assign this tutor to the lesson (ignore anything from frontend)
            Lesson lesson = lessonMapper.mapFrom(lessonDto);
            lesson.setTutorId(tutor.get().getId());

            lessonService.createLesson(lesson);
            return new ResponseEntity<>(lessonMapper.mapTo(lesson), HttpStatus.CREATED);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/lessons/{id}")
    public ResponseEntity<LessonDto> getLesson(@PathVariable("id") Long id) {
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

            Optional<Lesson> lessonSaved = lessonService.findLesson(id);
            if (lessonSaved.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Ensure the tutor owns this lesson
            if (!lessonSaved.get().getTutorId().equals(tutor.get().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            return ResponseEntity.ok(lessonMapper.mapTo(lessonSaved.get()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/lessons/student/{id}/pdf")
    public ResponseEntity<byte[]> getStudentLessonPdf(@PathVariable("id") Long id) {
        try {
            // 1. Authentication check
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // 2. Get student info
            String userEmail = authentication.getName();
            Student student = studentService.findStudent(userEmail).orElseThrow();

            // 3. Get the lesson and verify it belongs to this student
            Optional<Lesson> lessonOpt = lessonService.findLesson(id);
            if (lessonOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Lesson lesson = lessonOpt.get();

            // 4. Verify this lesson is assigned to the current student
            List<Lesson> studentLessons = lessonService.findLessonByStudent(student.getId());
            boolean lessonBelongsToStudent = studentLessons.stream()
                    .anyMatch(l -> l.getId().equals(id));

            if (!lessonBelongsToStudent) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 5. Get the file
            LessonFile file = lessonService.getFile(id);
            if (file == null || file.getData() == null) {
                return ResponseEntity.notFound().build();
            }

            // 6. Return the file
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"lesson-" + id + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(file.getData());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/lessons/tutor/me")
    public ResponseEntity<List<LessonDto>> findAllLessonsByCurrentTutor() {
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

            List<Lesson> lessons = lessonService.findLessonByTutor(tutor.get().getId());
            List<LessonDto> lessonDtos = lessons.stream()
                    .map(lessonMapper::mapTo)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(lessonDtos);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/lessons/student/they")
    public ResponseEntity<List<LessonDto>> findAllLessonsByStudent() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userEmail = authentication.getName();
            Optional<Student> student = Optional.ofNullable(studentService.findStudent(userEmail)).orElseThrow();

            if (student.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<Lesson> lessons = lessonService.findLessonByStudent(student.orElseThrow().getId());
            List<LessonDto> lessonDtos = lessons.stream()
                    .map(lessonMapper::mapTo)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(lessonDtos);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/lessons/student/{id}")
    public ResponseEntity<List<LessonDto>> findAllLessonsByStudentForCurrentTutor(
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

            List<Lesson> lessons = lessonService.findLessonByStudent(studentId);
            List<LessonDto> lessonDtos = lessons.stream()
                    .map(lessonMapper::mapTo)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(lessonDtos);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/lessons/{id}")
    public ResponseEntity<?> deleteLesson(@PathVariable("id") Long lessonId) {
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

            Optional<Lesson> lessonOpt = lessonService.findLesson(lessonId);

            if (lessonOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Lesson lesson = lessonOpt.get();

            // Ensure the tutor owns this lesson
            if (!lesson.getTutorId().equals(tutor.get().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            lessonService.deleteLesson(lessonId);
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete lesson"));
        }
    }

    @PatchMapping(path = "/lessons/{id}")
    public ResponseEntity<?> partialUpdate(
            @PathVariable Long id,
            @RequestBody LessonDto lessonDto) {
        try {
            // 1. Authenticate
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }

            String userEmail = authentication.getName();
            Optional<Tutor> tutorOpt = Optional.ofNullable(tutorService.findTutorByUserEmail(userEmail));
            if (tutorOpt.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }

            // 2. Check lesson exists and belongs to current tutor
            Optional<Lesson> existingLessonOpt = lessonService.findLesson(id);
            if (existingLessonOpt.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            Lesson existingLesson = existingLessonOpt.get();
            if (!existingLesson.getTutorId().equals(tutorOpt.get().getId())) {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }

            // 3. Perform update
            Lesson lessonToUpdate = lessonMapper.mapFrom(lessonDto);
            Lesson updatedLesson = lessonService.partialUpdate(id, lessonToUpdate);

            return new ResponseEntity<>(lessonMapper.mapTo(updatedLesson), HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update lesson");
        }
    }

    @GetMapping("/lessons/{id}/pdf")
    public ResponseEntity<byte[]> getLessonPdf(@PathVariable("id") Long id) {
        try {
            // 1. Authentication check
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // 2. Get tutor info
            String userEmail = authentication.getName();
            Tutor tutor = tutorService.findTutorByUserEmail(userEmail);
            if (tutor == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 3. Get the file
            LessonFile file = lessonService.getFile(id);
            if (file == null || file.getData() == null) {
                return ResponseEntity.notFound().build();
            }

            // 4. Verify tutor owns this lesson
            Optional<Lesson> lessonOpt = lessonService.findLesson(id);
            if (lessonOpt.isEmpty() || !lessonOpt.get().getTutorId().equals(tutor.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 5. Return the file
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"lesson-" + id + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(file.getData());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/lessons/{id}/pdf")
    public ResponseEntity<?> createLessonPdf(
            @PathVariable("id") Long id,
            @RequestParam("data") MultipartFile file) {
        try {
            // 1. Authentication check
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // 2. Get tutor info
            String userEmail = authentication.getName();
            Tutor tutor = tutorService.findTutorByUserEmail(userEmail);
            if (tutor == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 3. Verify lesson exists and belongs to tutor
            Optional<Lesson> lessonOpt = lessonService.findLesson(id);
            if (lessonOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Lesson lesson = lessonOpt.get();
            if (!lesson.getTutorId().equals(tutor.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 4. Process the file
            byte[] fileBytes = file.getBytes();
            lessonService.addFile(lesson, fileBytes);

            return ResponseEntity.ok().build();

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to process file");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/lessons/openAi")
    public ResponseEntity<byte[]> createLesson(@RequestBody String prompt, HttpServletRequest httpRequest) {
        try {
            System.out.println("[AI-Lesson] Request received");

            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                System.out.println("[AI-Lesson] Missing Authorization header");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Missing or invalid Authorization header".getBytes());
            }

            String token = authHeader.substring(7);
            String dbUserEmail = jwtService.extractUsername(token);
            Tutor tutor = tutorService.findTutorByUserEmail(dbUserEmail);

            if (tutor == null) {
                System.out.println("[AI-Lesson] Tutor not found for token user");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            if (tutor.getAiQueries() <= 0) {
                System.out.println("[AI-Lesson] Tutor exceeded AI query limit");
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body("No AI queries left".getBytes());
            }

            // Decrement AI queries early
            tutor.setAiQueries(tutor.getAiQueries() - 1);
            tutorService.saveTutor(tutor);
            System.out.println("[AI-Lesson] Tutor " + tutor.getId() + " query count decremented. Remaining: " + tutor.getAiQueries());

            // --- Timeout-safe AI call ---
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<String> aiFuture = executor.submit(() -> {
                System.out.println("[AI-Lesson] Sending prompt to OpenAI...");
                String response = chatClient.prompt()
                        .user("""
                        You are a professional LaTeX lesson generator.
                        Return ONLY compilable LaTeX code (no markdown, comments, or explanations).
                        Begin with \\documentclass and end with \\end{document}.
                        Include necessary packages like amsmath, pgfplots, and graphicx.
                        Content prompt:
                        """ + prompt)
                        .call()
                        .content();

                System.out.println("[AI-Lesson] OpenAI responded successfully.");
                return response;
            });

            String latexCode;
            try {
                latexCode = aiFuture.get(40, TimeUnit.SECONDS); // 40 sec timeout
            } catch (TimeoutException e) {
                executor.shutdownNow();
                System.out.println("[AI-Lesson] OpenAI request timed out");
                tutor.setAiQueries(tutor.getAiQueries() + 1);
                tutorService.saveTutor(tutor);
                return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                        .body("AI request timed out".getBytes());
            } finally {
                executor.shutdown();
            }

            if (latexCode == null || latexCode.isBlank()) {
                System.out.println("[AI-Lesson] OpenAI returned empty content");
                tutor.setAiQueries(tutor.getAiQueries() + 1);
                tutorService.saveTutor(tutor);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Empty response from AI".getBytes());
            }

            // --- Sanitize and prepare LaTeX ---
            System.out.println("[AI-Lesson] Cleaning LaTeX code...");
            latexCode = latexCode
                    .replaceAll("(?m)^```(?:latex)?\\s*", "")
                    .replaceAll("(?m)^```\\s*$", "")
                    .replaceAll("(?s).*?(\\\\documentclass)", "$1")
                    .trim();

            if (!latexCode.contains("\\documentclass")) {
                System.out.println("[AI-Lesson] Missing \\documentclass, inserting template");
                latexCode = "\\documentclass{article}\n\\usepackage{amsmath,graphicx,pgfplots}\n\\pgfplotsset{compat=1.18}\n\\begin{document}\n"
                        + latexCode + "\n\\end{document}";
            }

            if (!latexCode.contains("\\usepackage{pgfplots}")) {
                latexCode = latexCode.replaceFirst(
                        "(?<=\\\\documentclass\\{[^}]+\\})",
                        "\n\\\\usepackage{amsmath}\n\\\\usepackage{graphicx}\n\\\\usepackage{pgfplots}\n\\\\pgfplotsset{compat=1.18}\n"
                );
            }

            System.out.println("[AI-Lesson] Final LaTeX prepared:\n" + latexCode);

            // --- Compile LaTeX to PDF ---
            File tempDir = Files.createTempDirectory("latex").toFile();
            File texFile = new File(tempDir, "lesson.tex");
            Files.writeString(texFile.toPath(), latexCode);
            System.out.println("[AI-Lesson] Written .tex file to " + texFile.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(
                    "pdflatex", "-interaction=nonstopmode",
                    "-output-directory", tempDir.getAbsolutePath(),
                    texFile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String processOutput;
            try (InputStream is = process.getInputStream()) {
                processOutput = new String(is.readAllBytes());
            }
            int exitCode = process.waitFor();

            System.out.println("[AI-Lesson] pdflatex finished with exit code: " + exitCode);

            if (exitCode != 0) {
                System.err.println("[AI-Lesson] Compilation failed:\n" + processOutput);
                tutor.setAiQueries(tutor.getAiQueries() + 1);
                tutorService.saveTutor(tutor);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(("LaTeX compilation failed:\n" + processOutput).getBytes());
            }

            File pdfFile = new File(tempDir, "lesson.pdf");
            if (!pdfFile.exists()) {
                System.err.println("[AI-Lesson] PDF missing after compile");
                tutor.setAiQueries(tutor.getAiQueries() + 1);
                tutorService.saveTutor(tutor);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("PDF generation failed".getBytes());
            }

            byte[] pdfBytes = Files.readAllBytes(pdfFile.toPath());
            System.out.println("[AI-Lesson] PDF successfully generated (" + pdfBytes.length + " bytes)");

            // Cleanup
            Files.deleteIfExists(texFile.toPath());
            Files.deleteIfExists(pdfFile.toPath());
            Files.deleteIfExists(new File(tempDir, "lesson.aux").toPath());
            Files.deleteIfExists(new File(tempDir, "lesson.log").toPath());
            tempDir.delete();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment().filename("lesson.pdf").build());

            System.out.println("[AI-Lesson] Returning final PDF response.");
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            System.err.println("[AI-Lesson] ERROR: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error generating PDF: " + e.getMessage()).getBytes());
        }
    }
}