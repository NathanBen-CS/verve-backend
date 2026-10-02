package com.vervetutor.tutor_assistant.Student;

import com.vervetutor.tutor_assistant.Mappers.StudentMapper;
import com.vervetutor.tutor_assistant.Registration.RegistrationRequest;
import com.vervetutor.tutor_assistant.Registration.RegistrationService;
import com.vervetutor.tutor_assistant.Tutor.Tutor;
import com.vervetutor.tutor_assistant.Tutor.TutorService;
import com.vervetutor.tutor_assistant.User.AppUserRole;
import com.vervetutor.tutor_assistant.User.User;
import com.vervetutor.tutor_assistant.User.UserRepository;
import com.vervetutor.tutor_assistant.User.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class StudentController {

    private final StudentService studentService;
    private final StudentMapper studentMapper;
    private final RegistrationService registrationService;

    @Autowired
    private UserService userService;

    @Autowired
    private TutorService tutorService;

    @Autowired
    public StudentController(StudentService studentService, StudentMapper studentMapper, RegistrationService registrationService) {
        this.studentService = studentService;
        this.studentMapper = studentMapper;
        this.registrationService = registrationService;
    }

    @PostMapping("/students/me")
    public ResponseEntity<?> createStudentForCurrentTutor(@RequestBody StudentDto studentDto) {
        try {
            // 1. Authentication check
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized: no authentication found"));
            }

            // 2. Get current tutor
            String userEmail = authentication.getName();
            Optional<Tutor> tutorOpt = Optional.ofNullable(tutorService.findTutorByUserEmail(userEmail));
            if (tutorOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Forbidden: tutor not found"));
            }
            Tutor tutor = tutorOpt.get();

            // 3. Check student quota
            if (tutor.getStudentsLeft() <= 0) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Quota exceeded: no students left for this tutor"));
            }

            // 4. Create student
            Student student = studentMapper.mapFrom(studentDto);
            student.setTutor(tutor);

            RegistrationRequest request = RegistrationRequest.builder()
                    .email(studentDto.getEmail())
                    .appUserRole(AppUserRole.STUDENT)
                    .firstName(studentDto.getFirstName())
                    .lastName(studentDto.getLastName())
                    .password(studentDto.getPassword())
                    .build();

            registrationService.register(request);

            // 5. Update tutor quota
            tutor.setStudentsLeft(tutor.getStudentsLeft() - 1);
            tutorService.saveTutor(tutor);

            // 6. Save student
            studentService.createStudent(student);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(studentMapper.mapTo(student));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid input: " + e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // log exception to console for now (replace with logger if available)
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unexpected server error: " + e.getMessage()));
        }
    }


    @GetMapping("/students/me")
    public ResponseEntity<List<StudentDto>> findAllStudentsForCurrentTutor() {
        try {
            // 1. Authentication check
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // 2. Get current tutor
            String userEmail = authentication.getName();
            Optional<Tutor> tutorOpt = Optional.ofNullable(tutorService.findTutorByUserEmail(userEmail));
            if (tutorOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 3. Get students for this tutor
            List<Student> students = studentService.findStudentByTutor(tutorOpt.get().getId());
            List<StudentDto> studentDtos = students.stream()
                    .map(studentMapper::mapTo)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(studentDtos);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/students/me/{id}")
    public ResponseEntity<StudentDto> getStudentForCurrentTutor(@PathVariable("id") Long id) {
        try {
            // 1. Authentication check
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // 2. Get current tutor
            String userEmail = authentication.getName();
            Optional<Tutor> tutorOpt = Optional.ofNullable(tutorService.findTutorByUserEmail(userEmail));
            if (tutorOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 3. Get student and verify ownership
            Optional<Student> studentOpt = studentService.findStudent(id);
            if (studentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Student student = studentOpt.get();
            if (!student.getTutor().getId().equals(tutorOpt.get().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            return ResponseEntity.ok(studentMapper.mapTo(student));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @DeleteMapping("/students/me/{id}")
    public ResponseEntity<?> deleteStudentForCurrentTutor(@PathVariable("id") Long id) {
        try {
            // 1. Authentication check
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // 2. Get current tutor
            String userEmail = authentication.getName();
            Optional<Tutor> tutorOpt = Optional.ofNullable(tutorService.findTutorByUserEmail(userEmail));
            if (tutorOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 3. Get student and verify ownership
            Optional<Student> studentOpt = studentService.findStudent(id);
            if (studentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Student student = studentOpt.get();
            if (!student.getTutor().getId().equals(tutorOpt.get().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 4. Update tutor quota
            Tutor tutor = student.getTutor();
            tutor.setStudentsLeft(tutor.getStudentsLeft() + 1);
            tutorService.saveTutor(tutor);

            // 5. Delete student and student access as a user
            studentService.deleteStudent(id);
            User studentUser = userService.findByEmail(student.getEmail()).orElseThrow();
            userService.deleteUser(studentUser.getId());

            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete student"));
        }
    }

    @PatchMapping("/students/me/{id}")
    public ResponseEntity<?> partialUpdateForCurrentTutor(
            @PathVariable Long id,
            @RequestBody StudentDto studentDto) {
        try {
            // 1. Authentication check
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // 2. Get current tutor
            String userEmail = authentication.getName();
            Optional<Tutor> tutorOpt = Optional.ofNullable(tutorService.findTutorByUserEmail(userEmail));
            if (tutorOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 3. Get existing student and verify ownership
            Optional<Student> existingStudentOpt = studentService.findStudent(id);
            if (existingStudentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Student existingStudent = existingStudentOpt.get();
            if (!existingStudent.getTutor().getId().equals(tutorOpt.get().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 4. Perform update (ensure tutor association remains unchanged)
            Student studentToUpdate = studentMapper.mapFrom(studentDto);
            studentToUpdate.setTutor(existingStudent.getTutor()); // Maintain original tutor

            Student updatedStudent = studentService.partialUpdate(id, studentToUpdate);
            return ResponseEntity.ok(studentMapper.mapTo(updatedStudent));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update student");
        }
    }
}