$env:JAVA_HOME = "C:\Program Files\Java\jdk-22"
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "  QueueEase - Smart Digital Queue Management System" -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "Starting application on http://localhost:8080 ..." -ForegroundColor Green
Write-Host ""
& ".\.maven\apache-maven-3.9.6\bin\mvn.cmd" spring-boot:run
