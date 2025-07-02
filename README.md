# MuvTime - AI-Powered Fitness App

MuvTime is a comprehensive fitness application that combines AI-powered pose detection with gamified exercise tracking. Users can perform various exercises while the app uses computer vision to count repetitions, track progress, and award experience points.

## 🚀 Features

### Exercise Types
- **Jumps** - 20 reps → 20 XP
- **Arm Circles** - 20 reps → 20 XP  
- **High Knees** - 30 reps → 30 XP
- **Side Reaches** - 20 reps → 25 XP
- **Jack Jumps** - 12 reps → 20 XP
- **Biceps Curls** - 20 reps → 20 XP
- **Shoulder Press** - 20 reps → 20 XP
- **Squats** - 15 reps → 25 XP

### AI-Powered Pose Detection
- Real-time pose estimation using YOLO11/YOLOv8 models
- ONNX Runtime integration for optimized inference
- Automatic exercise recognition and rep counting
- Visual feedback with pose overlay

### Gamification System
- Experience Points (XP) for completed exercises
- Level progression system
- Exercise completion tracking
- Personal statistics and achievements

### User Experience
- Intuitive exercise tutorials with GIF demonstrations
- Real-time countdown and feedback
- Congratulations screens with XP rewards
- Comprehensive stats dashboard
- Clean, modern Material Design UI

## 🏗️ Architecture

### Frontend (Android)
- **Language**: Java
- **Framework**: Android SDK with CameraX
- **AI/ML**: ONNX Runtime for pose detection
- **UI**: Material Design components
- **Camera**: CameraX with real-time processing

### Backend (API)
- **Language**: C# (.NET 8)
- **Framework**: ASP.NET Core Web API
- **Database**: PostgreSQL via Supabase
- **Authentication**: Supabase Auth
- **Hosting**: Azure App Service

### Database
- **Provider**: Supabase (PostgreSQL)
- **ORM**: Entity Framework Core with Npgsql
- **Features**: Real-time subscriptions, Row Level Security

## 📱 Project Structure

```
FinalProjectTest/
├── app/                          # Android Application
│   ├── src/main/java/com/example/afinal/
│   │   ├── MainActivity.java     # Main exercise activity
│   │   ├── StatsActivity.java    # Statistics dashboard
│   │   ├── ApiService.java       # API communication
│   │   ├── *Counter.java         # Exercise detection classes
│   │   └── *TutorialActivity.java # Exercise tutorials
│   ├── src/main/assets/          # YOLO models and GIF assets
│   └── src/main/res/             # Android resources
├── MuvTimeAPI/                   # .NET Backend API
│   ├── Controllers/              # API controllers
│   ├── Models/                   # Data models and DTOs
│   ├── Services/                 # Business logic services
│   └── Program.cs               # API configuration
├── database_update.sql          # Database schema and functions
└── python_image_processing_file/ # YOLO training and testing
```

## 🛠️ Technology Stack

### Android App
- **Java** - Primary development language
- **Android SDK** - Native Android development
- **CameraX** - Modern camera API
- **ONNX Runtime** - AI model inference
- **Material Design** - UI/UX framework
- **SharedPreferences** - Local data storage
- **Retrofit/OkHttp** - HTTP client for API calls

### Backend API
- **C# .NET 8** - Backend language and framework
- **ASP.NET Core** - Web API framework
- **Entity Framework Core** - ORM
- **Npgsql** - PostgreSQL provider
- **Supabase SDK** - Database and auth integration
- **Azure App Service** - Cloud hosting

### Database & Infrastructure
- **Supabase** - Backend-as-a-Service
- **PostgreSQL** - Primary database
- **Azure** - Cloud hosting platform
- **Git** - Version control

## 🚀 Getting Started

### Prerequisites
- **Android Studio** (Latest version)
- **Java 8+**
- **.NET 8 SDK**
- **Supabase Account**
- **Azure Account** (for deployment)

### Android App Setup

1. **Clone the repository**
   ```bash
   git clone [repository-url]
   cd FinalProjectTest
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the project folder

3. **Configure API endpoint**
   ```java
   // In ApiService.java
   private static final String BASE_URL = "https://your-api-url.azurewebsites.net/";
   ```

4. **Add YOLO models**
   - Place `yolo11n-pose.pt` and `yolov8n-pose.pt` in `app/src/main/assets/`

5. **Build and run**
   ```bash
   ./gradlew assembleDebug
   ```

### Backend API Setup

1. **Navigate to API directory**
   ```bash
   cd MuvTimeAPI/MuvTimeAPI
   ```

2. **Install dependencies**
   ```bash
   dotnet restore
   ```

3. **Configure Supabase**
   ```json
   // In appsettings.json
   {
     "Supabase": {
       "Url": "https://your-project.supabase.co",
       "Key": "your-anon-key"
     }
   }
   ```

4. **Run locally**
   ```bash
   dotnet run
   ```

5. **Deploy to Azure**
   ```bash
   dotnet publish -c Release -o ./publish
   ```

### Database Setup

1. **Create Supabase project**
2. **Run database migrations**
   ```sql
   -- Execute database_update.sql in Supabase SQL editor
   ```

## 📊 API Endpoints

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration

### Statistics
- `GET /api/stats/{userId}` - Get user statistics
- `POST /api/stats/{userId}/update` - Update user stats after exercise
- `GET /api/stats/{userId}/sessions` - Get exercise session history

### Exercise Data
- `GET /api/stats/{userId}/summary` - Get exercise summary by type

## 🗄️ Database Schema

### Main Tables
- **user_stats** - User profile and aggregate statistics
- **exercise_sessions** - Individual exercise session records
- **user_profiles** - User account information

### Key Fields
```sql
user_stats:
- user_id (UUID, Primary Key)
- total_jumps, total_arm_circles, total_high_knees, etc.
- total_xp, level, exercises_completed
- created_at, updated_at

exercise_sessions:
- session_id (UUID, Primary Key)
- user_id (UUID, Foreign Key)
- exercise_type (VARCHAR)
- reps_completed (INTEGER)
- xp_earned (INTEGER)
- duration (INTEGER)
- completed_at (TIMESTAMP)
```

## 🎯 Exercise Detection Logic

Each exercise type has its own detection algorithm:

### Jump Detection
- Monitors hip keypoint vertical movement
- Detects takeoff and landing phases
- Filters noise with temporal smoothing

### Arm Circles
- Tracks shoulder and wrist keypoint positions
- Calculates circular motion patterns
- Counts complete rotations

### High Knees
- Monitors knee keypoint elevation
- Detects alternating leg lifts
- Counts rapid knee-to-chest movements

### Other Exercises
- Each exercise has specialized detection logic
- Optimized for accuracy and responsiveness
- Visual feedback for proper form

## 🎮 Gamification System

### XP Rewards
- Different exercises award different XP amounts
- Bonus XP for consistency and achievements
- Level progression unlocks new features

### Statistics Tracking
- Total exercises completed per type
- Personal records and streaks
- Progress visualization

## 🚀 Performance Optimizations

### AI Model Optimizations
1. **Reduced model input size** - 320x320 → optimized dimensions
2. **Direct tensor conversion** - Bypass bitmap intermediates
3. **Pre-allocated buffers** - Minimize garbage collection
4. **Temporal smoothing** - 5-frame history for stability
5. **Hardware acceleration** - GPU-optimized rendering

### API Optimizations
- Async/await patterns throughout
- Connection pooling for database
- Caching for frequently accessed data
- Compressed response payloads

## 🔧 Development

### Build Process
```bash
# Android
./gradlew assembleDebug

# Backend API  
dotnet build -c Release

# Database migrations
psql -f database_update.sql
```

### Testing
```bash
# Android unit tests
./gradlew test

# Backend API tests
dotnet test

# API endpoint testing
curl -X GET "https://your-api.azurewebsites.net/api/test"
```

## 📱 Screenshots

*(Add screenshots of your app here)*

- Main exercise screen with pose detection
- Stats dashboard showing progress
- Exercise tutorial screens
- Congratulations and XP reward screens

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **YOLO Team** - For the pose estimation models
- **Microsoft ONNX Runtime** - For mobile AI inference
- **Supabase** - For backend infrastructure
- **Azure** - For cloud hosting
- **Android CameraX Team** - For modern camera APIs

## 📞 Support

For support, email [your-email] or create an issue in this repository.

---

**MuvTime** - Making fitness fun with AI! 🏃‍♂️💪
