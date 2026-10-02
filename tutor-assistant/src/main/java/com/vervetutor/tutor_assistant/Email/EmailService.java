// Enhanced EmailService.java with modern, clean design
package com.vervetutor.tutor_assistant.Email;

import com.vervetutor.tutor_assistant.Lesson.Lesson;
import com.vervetutor.tutor_assistant.Student.Student;
import com.vervetutor.tutor_assistant.Student.StudentService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class EmailService implements EmailSender {

    private final static Logger LOGGER = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;
    private final StudentService studentService;

    @Override
    @Async
    public void send(String to, String email) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setText(email, true);
            helper.setTo(to);
            helper.setSubject("Confirm your email");
            helper.setFrom("hello@vervetutor.com");
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            LOGGER.error("failed to send email", e);
            throw new IllegalStateException("failed to send email");
        }
    }

    @Async
    public void sendDailySummary(String email, List<Lesson> tutorLessons) {
        try {
            String htmlContent = generateDailySummaryHtml(tutorLessons);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setText(htmlContent, true);
            helper.setTo(email);
            helper.setSubject("Daily Lesson Summary - " + java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            helper.setFrom("hello@vervetutor.com");

            mailSender.send(mimeMessage);
            LOGGER.info("Daily summary email sent successfully to: {}", email);
        } catch (MessagingException e) {
            LOGGER.error("Failed to send daily summary email to: {}", email, e);
            throw new IllegalStateException("Failed to send daily summary email");
        }
    }

    private String generateDailySummaryHtml(List<Lesson> lessons) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
        String currentDate = java.time.LocalDate.now().format(dateFormatter);

        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "  <head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Daily Lesson Summary</title>\n" +
                "  </head>\n" +
                "  <body style=\"margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f8fafc;\">\n" +
                "    <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin: 0; padding: 20px; background-color: #f8fafc;\">\n" +
                "      <tr>\n" +
                "        <td align=\"center\">\n" +
                "          <table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.08);\">\n" +
                "            \n" +
                "            <!-- Header -->\n" +
                "            <tr>\n" +
                "              <td style=\"background-color: #2563eb; padding: 40px; text-align: center; color: white;\">\n" +
                "                <h1 style=\"margin: 0; font-size: 24px; font-weight: 700;\">Daily Lesson Summary</h1>\n" +
                "                <p style=\"margin: 8px 0 0 0; font-size: 16px; opacity: 0.9;\">" + currentDate + "</p>\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "            \n" +
                "            <!-- Stats -->\n" +
                "            <tr>\n" +
                "              <td style=\"padding: 30px; background-color: #f8fafc;\">\n" +
                "                <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">\n" +
                "                  <tr>\n" +
                "                    <td width=\"50%\" style=\"padding-right: 10px;\">\n" +
                "                      <div style=\"background: white; padding: 20px; border-radius: 8px; text-align: center; border: 1px solid #e2e8f0;\">\n" +
                "                        <div style=\"font-size: 32px; font-weight: 700; color: #2563eb; margin-bottom: 4px;\">" + lessons.size() + "</div>\n" +
                "                        <div style=\"font-size: 14px; color: #64748b; font-weight: 500;\">Total Lessons</div>\n" +
                "                      </div>\n" +
                "                    </td>\n" +
                "                    <td width=\"50%\" style=\"padding-left: 10px;\">\n" +
                "                      <div style=\"background: white; padding: 20px; border-radius: 8px; text-align: center; border: 1px solid #e2e8f0;\">\n" +
                "                        <div style=\"font-size: 32px; font-weight: 700; color: #2563eb; margin-bottom: 4px;\">" + getTotalDuration(lessons) + "</div>\n" +
                "                        <div style=\"font-size: 14px; color: #64748b; font-weight: 500;\">Total Minutes</div>\n" +
                "                      </div>\n" +
                "                    </td>\n" +
                "                  </tr>\n" +
                "                </table>\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "            \n" +
                "            <!-- Lessons -->\n" +
                "            <tr>\n" +
                "              <td style=\"padding: 30px;\">\n" +
                (lessons.isEmpty() ?
                        "                <div style=\"text-align: center; padding: 40px; background-color: #f8fafc; border-radius: 8px; border: 2px dashed #cbd5e1;\">\n" +
                                "                  <h3 style=\"margin: 0 0 8px 0; font-size: 18px; font-weight: 600; color: #1e293b;\">No Lessons Today</h3>\n" +
                                "                  <p style=\"margin: 0; font-size: 14px; color: #64748b;\">You have no lessons scheduled for today.</p>\n" +
                                "                </div>\n"
                        : generateLessonsHtml(lessons, timeFormatter)) +
                "              </td>\n" +
                "            </tr>\n" +
                "            \n" +
                "            <!-- Footer -->\n" +
                "            <tr>\n" +
                "              <td style=\"padding: 30px; text-align: center; background-color: #f8fafc; border-top: 1px solid #e2e8f0;\">\n" +
                "                <a href=\"http://localhost:3000\" style=\"display: inline-block; background-color: #2563eb; color: white; text-decoration: none; padding: 12px 24px; border-radius: 6px; font-weight: 600; font-size: 14px; margin-bottom: 16px;\">\n" +
                "                  View Dashboard\n" +
                "                </a>\n" +
                "                <p style=\"margin: 0; font-size: 12px; color: #64748b;\">© 2025 VerveTutor. All rights reserved.</p>\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "            \n" +
                "          </table>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "    </table>\n" +
                "  </body>\n" +
                "</html>";
    }

    private String generateLessonsHtml(List<Lesson> lessons, DateTimeFormatter timeFormatter) {
        StringBuilder html = new StringBuilder();

        for (int i = 0; i < lessons.size(); i++) {
            Lesson lesson = lessons.get(i);
            Student student = getStudentForLesson(lesson);

            String timeDisplay = "TBD";
            try {
                if (lesson.getDateTime() != null) {
                    timeDisplay = lesson.getDateTimeAsLocalDateTime().format(timeFormatter);
                }
            } catch (Exception e) {
                timeDisplay = "TBD";
            }

            String statusColor = getStatusColor(lesson.getStatus());
            String statusBg = getStatusBackground(lesson.getStatus());

            html.append("                <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background: white; border: 1px solid #e2e8f0; border-radius: 8px; margin-bottom: ").append(i == lessons.size() - 1 ? "0" : "16px").append(";\">\n");
            html.append("                  <tr>\n");
            html.append("                    <td style=\"padding: 20px;\">\n");

            // Main lesson info
            html.append("                      <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">\n");
            html.append("                        <tr>\n");
            html.append("                          <td style=\"vertical-align: top;\">\n");
            html.append("                            <h3 style=\"margin: 0 0 4px 0; font-size: 16px; font-weight: 600; color: #1e293b;\">").append(lesson.getSubject() != null ? lesson.getSubject() : "Lesson").append("</h3>\n");
            html.append("                            <p style=\"margin: 0; font-size: 14px; color: #64748b;\">").append(timeDisplay).append(" • ").append(lesson.getDuration()).append(" minutes</p>\n");
            html.append("                          </td>\n");
            html.append("                          <td style=\"text-align: right; vertical-align: top;\">\n");
            html.append("                            <span style=\"background-color: ").append(statusBg).append("; color: ").append(statusColor).append("; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 500;\">").append(lesson.getStatus()).append("</span>\n");
            html.append("                          </td>\n");
            html.append("                        </tr>\n");

            // Student info
            if (student != null) {
                html.append("                        <tr>\n");
                html.append("                          <td colspan=\"2\" style=\"padding-top: 12px;\">\n");
                html.append("                            <div style=\"background-color: #f8fafc; padding: 12px; border-radius: 6px; border-left: 3px solid #2563eb;\">\n");
                html.append("                              <p style=\"margin: 0 0 2px 0; font-size: 14px; font-weight: 600; color: #1e293b;\">").append(getStudentFullName(student)).append("</p>\n");
                html.append("                              <p style=\"margin: 0; font-size: 12px; color: #64748b;\">").append(student.getEmail() != null ? student.getEmail() : "No email").append("</p>\n");
                html.append("                            </div>\n");
                html.append("                          </td>\n");
                html.append("                        </tr>\n");
            }

            html.append("                      </table>\n");
            html.append("                    </td>\n");
            html.append("                  </tr>\n");
            html.append("                </table>\n");
        }

        return html.toString();
    }

    private Student getStudentForLesson(Lesson lesson) {
        try {
            if (lesson.getStudentId() != null) {
                Optional<Student> student = studentService.findStudent(lesson.getStudentId());
                return student.orElse(null);
            }
        } catch (Exception e) {
            LOGGER.warn("Could not fetch student for lesson {}: {}", lesson.getId(), e.getMessage());
        }
        return null;
    }

    private String getStudentFullName(Student student) {
        if (student == null) return "Unknown Student";

        String firstName = student.getFirstName() != null ? student.getFirstName() : "";
        String lastName = student.getLastName() != null ? student.getLastName() : "";

        return (firstName + " " + lastName).trim();
    }

    private int getTotalDuration(List<Lesson> lessons) {
        return lessons.stream()
                .mapToInt(Lesson::getDuration)
                .sum();
    }

    private String getStatusColor(String status) {
        if (status == null) return "#64748b";

        switch (status.toLowerCase()) {
            case "completed":
                return "#059669";
            case "scheduled":
                return "#2563eb";
            case "cancelled":
                return "#dc2626";
            case "in_progress":
                return "#d97706";
            default:
                return "#64748b";
        }
    }

    private String getStatusBackground(String status) {
        if (status == null) return "#f1f5f9";

        switch (status.toLowerCase()) {
            case "completed":
                return "#dcfce7";
            case "scheduled":
                return "#dbeafe";
            case "cancelled":
                return "#fee2e2";
            case "in_progress":
                return "#fed7aa";
            default:
                return "#f1f5f9";
        }
    }
}