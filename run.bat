@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-22"
echo ===================================================
echo   QueueEase - Smart Digital Queue Management System
echo ===================================================
echo Starting application on http://localhost:8080 ...
echo.
call .\.maven\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
pause
