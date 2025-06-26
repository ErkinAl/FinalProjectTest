# MuvTime API

A .NET Web API backend for the MuvTime Android app, providing user statistics and exercise session tracking with Supabase integration.

## Features

- User statistics tracking (XP, level, jumps, exercises completed)
- Exercise session history
- Automatic level calculation (100 XP per level)
- RESTful API endpoints
- Supabase database integration
- CORS enabled for Android app

## API Endpoints

### Get User Stats
```
GET /api/stats/{userId}
```
Returns user statistics including level, XP, total jumps, and exercises completed.

**Response:**
```json
{
  "level": 0,
  "xp": 0,
  "totalJumps": 0,
  "exercisesCompleted": 0,
  "xpToNextLevel": 100,
  "currentLevelXp": 0
}
```

### Update User Stats
```
POST /api/stats/{userId}/update
```
Updates user statistics after completing an exercise.

**Request Body:**
```json
{
  "jumpsCompleted": 20,
  "xpEarned": 20,
  "sessionDuration": 60
}
```

**Response:**
```json
{
  "level": 0,
  "xp": 20,
  "totalJumps": 20,
  "exercisesCompleted": 1,
  "xpToNextLevel": 80,
  "currentLevelXp": 20
}
```

### Get User Sessions
```
GET /api/stats/{userId}/sessions?limit=10
```
Returns the user's exercise session history.

**Response:**
```json
[
  {
    "id": "uuid",
    "exerciseType": "jump_counter",
    "jumpsCompleted": 20,
    "xpEarned": 20,
    "sessionDuration": 60,
    "completedAt": "2025-06-27T01:30:00Z"
  }
]
```

### Reset User Stats
```
POST /api/stats/{userId}/reset
```
Resets all user statistics to zero.

**Response:**
```json
{
  "message": "Stats reset successfully"
}
```

## Configuration

Update `appsettings.json` with your Supabase credentials:

```json
{
  "Supabase": {
    "Url": "https://your-project.supabase.co",
    "Key": "your-anon-key",
    "ServiceKey": "your-service-key"
  }
}
```

## Database Schema

The API expects the following Supabase tables:

### user_stats
```sql
CREATE TABLE user_stats (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id TEXT NOT NULL,
  xp INTEGER DEFAULT 0,
  level INTEGER DEFAULT 0,
  total_jumps INTEGER DEFAULT 0,
  exercises_completed INTEGER DEFAULT 0,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

### exercise_sessions
```sql
CREATE TABLE exercise_sessions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id TEXT NOT NULL,
  exercise_type TEXT DEFAULT 'jump_counter',
  jumps_completed INTEGER DEFAULT 0,
  xp_earned INTEGER DEFAULT 0,
  session_duration INTEGER DEFAULT 0,
  completed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

## Running the API

1. Install .NET 8 SDK
2. Configure Supabase credentials in `appsettings.json`
3. Run the API:
   ```bash
   dotnet run
   ```
4. The API will be available at:
   - HTTPS: https://localhost:7291
   - HTTP: http://localhost:5129
   - Swagger UI: https://localhost:7291/swagger

## Testing

Run the test script to verify API functionality:
```powershell
.\test-api.ps1
```

## Level System

- Users start at Level 0 with 0 XP
- Each level requires 100 XP
- Level = Total XP ÷ 100 (integer division)
- XP to next level = 100 - (Total XP % 100)

## Android Integration

The API is configured with CORS to allow requests from the Android app. Use the following base URL in your Android app:

```
https://your-api-url/api/stats/
```

Replace `your-api-url` with your deployed API URL or use `https://localhost:7291` for local testing. 