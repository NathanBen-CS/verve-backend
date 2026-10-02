FROM eclipse-temurin:17-jdk

# Install LaTeX and clean cache
RUN apt-get update && \
    apt-get install -y texlive-latex-base texlive-latex-recommended texlive-latex-extra texlive-fonts-recommended && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY target/tutor-assistant.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
