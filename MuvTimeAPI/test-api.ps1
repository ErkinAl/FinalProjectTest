# Test script for MuvTime API
Write-Host "Testing MuvTime API..." -ForegroundColor Green

# Wait for API to start
Start-Sleep -Seconds 3

# Test 1: Get user stats (should create new user)
Write-Host "`n1. Testing GET /api/stats/test-user" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "https://localhost:7291/api/stats/test-user" -Method GET -SkipCertificateCheck
    Write-Host "Response:" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 3
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 2: Update user stats
Write-Host "`n2. Testing POST /api/stats/test-user/update" -ForegroundColor Yellow
$updateData = @{
    JumpsCompleted = 20
    XpEarned = 20
    SessionDuration = 60
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "https://localhost:7291/api/stats/test-user/update" -Method POST -Body $updateData -ContentType "application/json" -SkipCertificateCheck
    Write-Host "Response:" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 3
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 3: Get user sessions
Write-Host "`n3. Testing GET /api/stats/test-user/sessions" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "https://localhost:7291/api/stats/test-user/sessions" -Method GET -SkipCertificateCheck
    Write-Host "Response:" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 3
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 4: Get updated stats
Write-Host "`n4. Testing GET /api/stats/test-user (after update)" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "https://localhost:7291/api/stats/test-user" -Method GET -SkipCertificateCheck
    Write-Host "Response:" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 3
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`nAPI testing completed!" -ForegroundColor Green 