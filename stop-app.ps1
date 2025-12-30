# Stop Hyperswitch Payment Service
Write-Host "Stopping Hyperswitch Payment Service..." -ForegroundColor Yellow

# Find Java processes on port 8080
$portInfo = netstat -ano | findstr :8080
if ($portInfo) {
    $lines = $portInfo -split "`n"
    foreach ($line in $lines) {
        if ($line -match '\s+(\d+)$') {
            $pid = $matches[1]
            Write-Host "Found process on port 8080 with PID: $pid" -ForegroundColor Green
            try {
                taskkill /PID $pid /F 2>$null
                Write-Host "Process $pid stopped successfully!" -ForegroundColor Green
            } catch {
                Write-Host "Failed to stop process $pid" -ForegroundColor Red
            }
        }
    }
} else {
    Write-Host "No process found on port 8080. Application may already be stopped." -ForegroundColor Yellow
}

# Also check for Java processes that might be the payment service
Write-Host "`nChecking for Java processes..." -ForegroundColor Yellow
$javaProcesses = Get-Process -Name java -ErrorAction SilentlyContinue
if ($javaProcesses) {
    Write-Host "Found $($javaProcesses.Count) Java process(es) running." -ForegroundColor Yellow
    Write-Host "If the application is still running, you may need to stop it manually from your IDE or terminal." -ForegroundColor Yellow
} else {
    Write-Host "No Java processes found." -ForegroundColor Green
}

# Verify
Start-Sleep -Seconds 2
Write-Host "`nVerifying..." -ForegroundColor Yellow
$check = netstat -ano | findstr :8080
if ($check) {
    Write-Host "Warning: Port 8080 is still in use!" -ForegroundColor Red
    Write-Host "You may need to stop the application from your IDE or restart your computer." -ForegroundColor Yellow
} else {
    Write-Host "Confirmed: Port 8080 is free. Application appears to be stopped." -ForegroundColor Green
}

