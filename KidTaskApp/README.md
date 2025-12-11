# KidTask - Task and Wish Management Application

A Java-based GUI application that allows children to manage their daily/weekly tasks, track points, and handle wishes (products or activities) with parental or teacher approval.

## Features

### User Roles
- **Child**: Views and completes tasks, adds wishes, tracks points and level
- **Parent**: Adds/approves tasks and wishes, monitors progress
- **Teacher**: Adds school-related tasks and rates completed ones

### Core Functionality

#### Task Management
- Add new tasks (title, description, due date, points)
- Display all tasks with filters
- Mark tasks as completed (child)
- Approve and rate completed tasks (parent/teacher)
- Automatically update child's points and level

#### Wish Management
- Add product or activity wishes (child)
- Approve/reject wishes (parent)
- Only display wishes available at or above the child's level

#### Points & Level Tracking
- Display child's total points and level visually (progress bar)
- Update level dynamically based on average ratings
- View ratings history and task summaries

#### Data Persistence
- Store all data (tasks, wishes, users) in JSON format
- Automatic save on changes
- Data files stored in `data/` directory:
  - `Users.txt` - User accounts
  - `Tasks.txt` - All tasks
  - `Wishes.txt` - All wishes

## Project Structure

```
src/main/java/com/kidtask/
├── KidTaskApp.java          # Main application entry point
├── models/                  # Data model classes
│   ├── User.java
│   ├── Child.java
│   ├── Parent.java
│   ├── Teacher.java
│   ├── Task.java
│   ├── Wish.java
│   ├── UserRole.java
│   ├── TaskStatus.java
│   └── WishStatus.java
├── data/                    # Data management
│   └── DataManager.java
└── gui/                     # GUI components
    ├── LoginFrame.java
    ├── DashboardFrame.java
    ├── TaskPanel.java
    ├── WishPanel.java
    └── ProgressPanel.java
```

## Requirements

- Java 11 or higher
- Maven 3.6 or higher
- IDE: IntelliJ IDEA or Eclipse (recommended)

## Building and Running

### Using Maven

1. **Build the project:**
   ```bash
   mvn clean compile
   ```

2. **Run the application:**
   ```bash
   mvn exec:java -Dexec.mainClass="com.kidtask.KidTaskApp"
   ```

3. **Create executable JAR:**
   ```bash
   mvn clean package
   java -jar target/kidtask-1.0.0.jar
   ```

### Using IDE

1. Import the project as a Maven project
2. Wait for Maven to download dependencies
3. Run `KidTaskApp.java` as the main class

## Usage

### First Time Setup

1. Launch the application
2. Register a new user:
   - Enter username and password
   - Select role (CHILD, PARENT, or TEACHER)
   - Click "Register"
3. Login with your credentials

### For Children

- **View Tasks**: See all assigned tasks in the Tasks tab
- **Complete Tasks**: Select a pending task and click "Mark Completed"
- **Add Wishes**: Fill in wish details and click "Add Wish"
- **View Progress**: Check points, level, and ratings in the Progress tab

### For Parents

- **Add Tasks**: Create tasks and assign them to children
- **Approve Tasks**: Review completed tasks, rate them (0-5), and approve
- **Manage Wishes**: Approve or reject wishes from children
- **Monitor Progress**: View all children's progress in the Progress tab

### For Teachers

- **Add Tasks**: Create school-related tasks and assign to children
- **Rate Tasks**: Review and rate completed tasks (0-5)
- **View Progress**: Monitor children's academic progress

## Technical Details

- **GUI Framework**: Java Swing
- **Data Format**: JSON (using Gson library)
- **Architecture**: Object-Oriented with separation of concerns
- **Design Patterns**: MVC-like structure with separate models, data, and GUI layers

## Data Files

All data is stored in the `data/` directory:
- Files are created automatically on first run
- Data persists between sessions
- JSON format for easy readability and debugging

## Notes

- Passwords are stored in plain text (for simplicity in this educational project)
- Task IDs and Wish IDs are generated using UUID
- Level calculation: Based on average rating (1-5 rating maps to 1-5 level)
- Only wishes with required level <= child's current level are visible to children

## License

This project is created for educational purposes as part of SENG 383 Software Project course.

