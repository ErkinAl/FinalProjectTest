# Simple API test script
Write-Host "Testing API connection..." -ForegroundColor Green

# Test basic connection
try {
    $response = Invoke-WebRequest -Uri "http://localhost:5129/swagger/v1/swagger.json" -Method GET
    Write-Host "✅ API is running and Swagger is accessible" -ForegroundColor Green
    Write-Host "Status: $($response.StatusCode)" -ForegroundColor Yellow
} catch {
    Write-Host "❌ API connection failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test stats endpoint with error details
Write-Host "`nTesting stats endpoint..." -ForegroundColor Green
try {
    $response = Invoke-WebRequest -Uri "http://localhost:5129/api/stats/test-user" -Method GET
    Write-Host "✅ Stats endpoint working" -ForegroundColor Green
    Write-Host "Response: $($response.Content)" -ForegroundColor Yellow
} catch {
    Write-Host "❌ Stats endpoint failed" -ForegroundColor Red
    Write-Host "Status Code: $($_.Exception.Response.StatusCode)" -ForegroundColor Red
    
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $errorContent = $reader.ReadToEnd()
        Write-Host "Error Content: $errorContent" -ForegroundColor Red
    }
}

Write-Host "`nTest completed!" -ForegroundColor Green 