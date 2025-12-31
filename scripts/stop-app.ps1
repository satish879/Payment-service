# Stop Hyperswitch Payment Service
Write-Host "`n=== Stopping Hyperswitch Payment Service ===" -ForegroundColor Cyan

$stopped = $false

# Method 1: Find and stop processes using port 8080
Write-Host "`n[1/3] Checking for processes on port 8080..." -ForegroundColor Yellow
try {
    $connections = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
    if ($connections) {
        $pids = $connections | Select-Object -ExpandProperty OwningProcess -Unique
        foreach ($pid in $pids) {
            try {
                $process = Get-Process -Id $pid -ErrorAction SilentlyContinue
                if ($process) {
                    Write-Host "  Found process: $($process.ProcessName) (PID: $pid)" -ForegroundColor Yellow
                    Stop-Process -Id $pid -Force -ErrorAction Stop
                    Write-Host "  [OK] Process $pid stopped successfully!" -ForegroundColor Green
                    $stopped = $true
                }
            } catch {
                Write-Host "  [ERROR] Failed to stop process $pid : $($_.Exception.Message)" -ForegroundColor Red
            }
        }
    } else {
        Write-Host "  No process found on port 8080." -ForegroundColor Gray
    }
} catch {
    Write-Host "  Error checking port 8080: $($_.Exception.Message)" -ForegroundColor Red
}

# Method 2: Find and stop Java processes listening on port 8080
Write-Host "`n[2/3] Checking for Java processes on port 8080..." -ForegroundColor Yellow
try {
    $allJava = Get-Process -Name java -ErrorAction SilentlyContinue
    $javaProcesses = @()
    foreach ($proc in $allJava) {
        $listening = Get-NetTCPConnection -OwningProcess $proc.Id -LocalPort 8080 -ErrorAction SilentlyContinue
        if ($listening) {
            $javaProcesses += $proc
        }
    }
    
    if ($javaProcesses) {
        foreach ($proc in $javaProcesses) {
            try {
                Write-Host "  Found Java process: PID $($proc.Id)" -ForegroundColor Yellow
                Stop-Process -Id $proc.Id -Force -ErrorAction Stop
                Write-Host "  [OK] Java process $($proc.Id) stopped successfully!" -ForegroundColor Green
                $stopped = $true
            } catch {
                Write-Host "  [ERROR] Failed to stop Java process $($proc.Id) : $($_.Exception.Message)" -ForegroundColor Red
            }
        }
    } else {
        Write-Host "  No payment service Java processes found." -ForegroundColor Gray
    }
} catch {
    Write-Host "  Error checking Java processes: $($_.Exception.Message)" -ForegroundColor Red
}

# Method 3: Fallback - stop all Java processes if they're using port 8080
Write-Host "`n[3/3] Final check for any remaining processes on port 8080..." -ForegroundColor Yellow
Start-Sleep -Seconds 1
try {
    $remaining = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
    if ($remaining) {
        $remainingPids = $remaining | Select-Object -ExpandProperty OwningProcess -Unique
        foreach ($pid in $remainingPids) {
            try {
                $process = Get-Process -Id $pid -ErrorAction SilentlyContinue
                if ($process) {
                    Write-Host "  Found remaining process: $($process.ProcessName) (PID: $pid)" -ForegroundColor Yellow
                    Stop-Process -Id $pid -Force -ErrorAction Stop
                    Write-Host "  [OK] Process $pid stopped!" -ForegroundColor Green
                    $stopped = $true
                }
            } catch {
                Write-Host "  [ERROR] Could not stop process $pid : $($_.Exception.Message)" -ForegroundColor Red
            }
        }
    }
} catch {
    Write-Host "  Error in final check: $($_.Exception.Message)" -ForegroundColor Red
}

# Final verification
Write-Host "`n=== Verification ===" -ForegroundColor Cyan
Start-Sleep -Seconds 2
$finalCheck = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($finalCheck) {
    Write-Host "[WARNING] Port 8080 is still in use!" -ForegroundColor Red
    $finalPids = $finalCheck | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($pid in $finalPids) {
        $proc = Get-Process -Id $pid -ErrorAction SilentlyContinue
        if ($proc) {
            Write-Host "  Process still running: $($proc.ProcessName) (PID: $pid)" -ForegroundColor Yellow
        }
    }
    Write-Host "`nYou may need to:" -ForegroundColor Yellow
    Write-Host "  1. Stop the application from your IDE" -ForegroundColor Yellow
    Write-Host "  2. Close the terminal window where it is running" -ForegroundColor Yellow
    Write-Host "  3. Restart your computer if the process persists" -ForegroundColor Yellow
} else {
    Write-Host "[OK] Port 8080 is free. Application stopped successfully!" -ForegroundColor Green
    $stopped = $true
}

if ($stopped) {
    Write-Host "`n[OK] Payment Service has been stopped." -ForegroundColor Green
} else {
    Write-Host "`n[INFO] No running instances of Payment Service were found." -ForegroundColor Cyan
}

Write-Host ""
