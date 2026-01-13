# Start Hyperswitch Payment Service
Write-Host "`n=== Starting Hyperswitch Payment Service ===" -ForegroundColor Cyan

# Get script directory
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
# Repository root (parent of scripts directory)
$repoRoot = Split-Path -Parent $scriptDir
$jarPath = Join-Path $repoRoot "paymentservice\paymentservice-web\target\paymentservice-web-1.0.0-SNAPSHOT.jar"

# Check if JAR exists
if (-not (Test-Path $jarPath)) {
    Write-Host "[ERROR] JAR file not found at: $jarPath" -ForegroundColor Red
    Write-Host "Please build the application first:" -ForegroundColor Yellow
    Write-Host "  cd paymentservice" -ForegroundColor Yellow
    Write-Host "  mvn clean package spring-boot:repackage -DskipTests" -ForegroundColor Yellow
    exit 1
}

# Check if port 8080 is already in use
Write-Host "`nChecking if port 8080 is available..." -ForegroundColor Yellow
$portCheck = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($portCheck) {
    Write-Host "[WARNING] Port 8080 is already in use!" -ForegroundColor Red
    Write-Host "Please stop the existing application first using: .\stop-app.ps1" -ForegroundColor Yellow
    $response = Read-Host "Do you want to stop it now? (Y/N)"
    if ($response -eq "Y" -or $response -eq "y") {
        & (Join-Path $scriptDir "stop-app.ps1")
        Start-Sleep -Seconds 2
    } else {
        Write-Host "Exiting..." -ForegroundColor Yellow
        exit 1
    }
} else {
    Write-Host "[OK] Port 8080 is available." -ForegroundColor Green
}

# Start the application
Write-Host "`nStarting application..." -ForegroundColor Yellow
Write-Host "JAR Location: $jarPath" -ForegroundColor Gray
Write-Host "Port: 8080" -ForegroundColor Gray
Write-Host "`nApplication is starting..." -ForegroundColor Cyan
Write-Host "Press Ctrl+C to stop the application" -ForegroundColor Yellow
Write-Host "`nOnce started, you can access:" -ForegroundColor Cyan
Write-Host "  Health: http://localhost:8080/api/health" -ForegroundColor White
Write-Host "  Swagger: http://localhost:8080/swagger-ui.html" -ForegroundColor White
Write-Host "`n" -ForegroundColor White

# Change to the target directory and run in foreground
$targetDir = Join-Path $repoRoot "paymentservice\paymentservice-web\target"
Set-Location $targetDir
java "-Dspring.classformat.ignore=true" -jar paymentservice-web-1.0.0-SNAPSHOT.jar

