package com.vervetutor.tutor_assistant.Registration;

import com.vervetutor.tutor_assistant.Email.EmailSender;
import com.vervetutor.tutor_assistant.Registration.Token.ConfirmationToken;
import com.vervetutor.tutor_assistant.Registration.Token.ConfirmationTokenService;
import com.vervetutor.tutor_assistant.Tutor.Tutor;
import com.vervetutor.tutor_assistant.Tutor.TutorService;
import com.vervetutor.tutor_assistant.User.AppUserRole;
import com.vervetutor.tutor_assistant.User.User;
import com.vervetutor.tutor_assistant.User.UserService;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class RegistrationService {

    private final UserService userService;
    private EmailValidator emailValidator;
    private ConfirmationTokenService confirmationTokenService;
    private final EmailSender emailSender;

    private final TutorService tutorService;

    public String register(RegistrationRequest request) {
        boolean isValidEmail = emailValidator.test(request.getEmail());
        if (!isValidEmail)
        {
            throw new IllegalStateException("email not valid");
        }

        String token = userService.signUpUser(
                new User(
                        request.getFirstName(),
                        request.getLastName(),
                        request.getEmail(),
                        request.getPassword(),
                        request.getAppUserRole()
                ));
        String link = "https://vervetutor-220853925149.us-central1.run.app/registration/confirm?token=" + token;
        emailSender.send(request.getEmail(), buildEmail(request.getFirstName(), link));

        return token;
    }

    @Transactional
    public String confirmToken(String token)
    {
        ConfirmationToken confirmationToken = confirmationTokenService.getToken(token).orElseThrow(() -> new IllegalStateException("Token not found"));
        if (confirmationToken.getConfirmedAt() != null){
            throw new IllegalStateException("email already confirmed");
        }

        LocalDateTime expiredAt = confirmationToken.getExpiresAt();
        if (LocalDateTime.now().isAfter(expiredAt))
        {
            throw new IllegalStateException("token expired");
        }

        confirmationToken.setConfirmedAt(LocalDateTime.now());

        //Not sure how this should acc be with a built in method
        userService.enableUser(confirmationToken.getUser().getEmail());

        if (confirmationToken.getUser().getAppUserRole() == AppUserRole.USER)
        {
            //here the user gets access to its own tutor portal CHANGE WHEN PAYMENTS WORK
            Tutor tutor = Tutor.builder().firstName(confirmationToken.getUser().getFirstName())
                    .lastName(confirmationToken.getUser().getLastName()).build();
            User user = userService.getUserById(confirmationToken.getUser().getId()).orElseThrow();
            user.setTutor(tutor);
            tutorService.createTutor(tutor);
        }


        confirmationToken.getUser().setEnabled(true);

        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "  <head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Confirmation Successful</title>\n" +
                "  </head>\n" +
                "  <body style=\"margin: 0; padding: 0; font-family: 'Segoe UI', Arial, sans-serif; background-color: #f8fafc; min-height: 100vh;\">\n" +
                "    <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"max-width: 600px; margin: 50px auto; background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.08); overflow: hidden; border: 1px solid #e2e8f0;\">\n" +
                "      <!-- Header Section -->\n" +
                "      <tr>\n" +
                "        <td style=\"padding: 0; text-align: center; background-color: #4285f4; position: relative;\">\n" +
                "          <div style=\"padding: 40px 30px; position: relative; z-index: 2;\">\n" +
                "            <div style=\"width: 70px; height: 70px; background-color: #ffffff; border-radius: 50%; margin: 0 auto 20px; display: flex; align-items: center; justify-content: center; box-shadow: 0 4px 12px rgba(0,0,0,0.15);\">\n" +
                "              <svg width=\"32\" height=\"32\" viewBox=\"0 0 24 24\" fill=\"none\" style=\"color: #4285f4;\">\n" +
                "                <path d=\"M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/>\n" +
                "              </svg>\n" +
                "            </div>\n" +
                "            <h1 style=\"color: #ffffff; margin: 0; font-size: 26px; font-weight: 600;\">Email Confirmed!</h1>\n" +
                "            <p style=\"color: rgba(255,255,255,0.9); margin: 12px 0 0 0; font-size: 15px; font-weight: 400;\">Welcome to VerveTutor</p>\n" +
                "          </div>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "      \n" +
                "      <!-- Main Content -->\n" +
                "      <tr>\n" +
                "        <td style=\"padding: 40px 35px; text-align: center; background-color: #ffffff;\">\n" +
                "          <div style=\"max-width: 400px; margin: 0 auto;\">\n" +
                "            <h2 style=\"color: #1f2937; margin: 0 0 16px 0; font-size: 20px; font-weight: 600;\">Success!</h2>\n" +
                "            <p style=\"font-size: 15px; color: #6b7280; line-height: 1.5; margin-bottom: 24px;\">Your email address has been successfully verified. You now have full access to all VerveTutor features.</p>\n" +
                "            \n" +
                "            <!-- Feature highlights -->\n" +
                "            <div style=\"text-align: left; margin: 28px 0; padding: 20px; background-color: #f8fafc; border-radius: 8px; border-left: 4px solid #4285f4;\">\n" +
                "              <div style=\"margin-bottom: 12px;\">\n" +
                "                <div style=\"width: 6px; height: 6px; background-color: #4285f4; border-radius: 50%; display: inline-block; margin-right: 10px;\"></div>\n" +
                "                <span style=\"color: #4b5563; font-size: 14px;\">Streamline student management with centralized profiles</span>\n" +
                "              </div>\n" +
                "              <div style=\"margin-bottom: 12px;\">\n" +
                "                <div style=\"width: 6px; height: 6px; background-color: #4285f4; border-radius: 50%; display: inline-block; margin-right: 10px;\"></div>\n" +
                "                <span style=\"color: #4b5563; font-size: 14px;\">Build engaging lesson plans with rich multimedia content</span>\n" +
                "              </div>\n" +
                "              <div style=\"margin-bottom: 12px;\">\n" +
                "                <div style=\"width: 6px; height: 6px; background-color: #4285f4; border-radius: 50%; display: inline-block; margin-right: 10px;\"></div>\n" +
                "                <span style=\"color: #4b5563; font-size: 14px;\">Your complete teaching hub with smart scheduling & reminders</span>\n" +
                "              </div>\n" +
                "              <div>\n" +
                "                <div style=\"width: 6px; height: 6px; background-color: #4285f4; border-radius: 50%; display: inline-block; margin-right: 10px;\"></div>\n" +
                "                <span style=\"color: #4b5563; font-size: 14px;\">Students can revisit and review completed lessons instantly</span>\n" +
                "              </div>\n" +
                "            </div>\n" +
                "            \n" +
                "            <div style=\"margin-top: 32px;\">\n" +
                "              <a href=\"https://vervetutor.com\" style=\"display: inline-block; background-color: #4285f4; color: #ffffff; text-decoration: none; padding: 14px 32px; border-radius: 6px; font-weight: 500; font-size: 15px; box-shadow: 0 2px 8px rgba(66, 133, 244, 0.2); transition: all 0.3s ease;\">\n" +
                "                Continue to Dashboard\n" +
                "              </a>\n" +
                "            </div>\n" +
                "            \n" +
                "            <p style=\"font-size: 13px; color: #9ca3af; margin-top: 24px; margin-bottom: 0;\">\n" +
                "              Having trouble? <a href=\"mailto:vervetutor@gmail.com\" style=\"color: #4285f4; text-decoration: none; font-weight: 500;\">Contact Support</a>\n" +
                "            </p>\n" +
                "          </div>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "      \n" +
                "      <!-- Footer -->\n" +
                "      <tr>\n" +
                "        <td style=\"padding: 24px 35px; text-align: center; background-color: #f8fafc; border-top: 1px solid #e5e7eb;\">\n" +
                "          <div style=\"margin-bottom: 16px;\">\n" +
                "            <a href=\"#\" style=\"color: #9ca3af; text-decoration: none; font-size: 13px; margin: 0 12px;\">Privacy Policy</a>\n" +
                "            <span style=\"color: #d1d5db;\">|</span>\n" +
                "            <a href=\"#\" style=\"color: #9ca3af; text-decoration: none; font-size: 13px; margin: 0 12px;\">Terms of Service</a>\n" +
                "            <span style=\"color: #d1d5db;\">|</span>\n" +
                "            <a href=\"#\" style=\"color: #9ca3af; text-decoration: none; font-size: 13px; margin: 0 12px;\">Help Center</a>\n" +
                "          </div>\n" +
                "          <p style=\"font-size: 12px; color: #9ca3af; margin: 0; line-height: 1.4;\">\n" +
                "            &copy; 2025 VerveTutor. All rights reserved.<br>\n" +
                "            <span style=\"font-size: 11px;\">1234 Learning Street, Education City, EC 12345</span>\n" +
                "          </p>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "    </table>\n" +
                "  </body>\n" +
                "</html>";
    }

    private String buildEmail(String name, String link) {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "  <head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Email Confirmation</title>\n" +
                "  </head>\n" +
                "  <body style=\"margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f8fb;\">\n" +
                "    <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); margin-top: 20px;\">\n" +
                "      <tr>\n" +
                "        <td style=\"padding: 30px 0; text-align: center; background-color: #1e88e5; border-top-left-radius: 8px; border-top-right-radius: 8px;\">\n" +
                "          <h1 style=\"color: #ffffff; margin: 0;\">Email Confirmation</h1>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "      <tr>\n" +
                "        <td style=\"padding: 40px 30px;\">\n" +
                "          <p style=\"font-size: 16px; color: #333333; margin-bottom: 20px;\">Hi " + name + ",</p>\n" +
                "          <p style=\"font-size: 16px; color: #333333; margin-bottom: 20px;\">Thank you for registering. Please click on the link below to activate your account:</p>\n" +
                "          <p style=\"text-align: center; margin-bottom: 30px; margin-top: 30px;\">\n" +
                "            <a href=\"" + link + "\" style=\"display: inline-block; background-color: #1e88e5; color: #ffffff; text-decoration: none; padding: 12px 30px; border-radius: 4px; font-weight: bold; font-size: 16px;\">Activate Account</a>\n" +
                "          </p>\n" +
                "          <p style=\"font-size: 16px; color: #333333; margin-bottom: 20px;\">If you didn't request this, please ignore this email.</p>\n" +
                "          <p style=\"font-size: 16px; color: #333333;\">Thank you,<br>The VerveTutor Team</p>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "      <tr>\n" +
                "        <td style=\"padding: 20px; text-align: center; background-color: #f0f7ff; border-bottom-left-radius: 8px; border-bottom-right-radius: 8px;\">\n" +
                "          <p style=\"font-size: 14px; color: #6c757d; margin: 0;\">&copy; 2025 VerveTutor. All rights reserved.</p>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "    </table>\n" +
                "  </body>\n" +
                "</html>";
    }
}
