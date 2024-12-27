# Face Recognition System

A Java application that can recognize faces and store them in a database. The system can:
1. Detect faces using your webcam
2. Add new faces to the database
3. Recognize known faces in real-time

## Prerequisites
- Java 11 or higher
- Maven
- Webcam
- SQLite (automatically managed by the application)

## Project Structure
```
windsurfproj/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── windsurfproject/
│                   ├── FaceRecognitionApp.java
│                   ├── DatabaseHandler.java
│                   └── FaceData.java
├── pom.xml
└── README.md
```

## How to Run
1. Make sure you have Maven installed
2. Download the Haar Cascade classifier file:
   ```
   curl -o haarcascade_frontalface_default.xml https://raw.githubusercontent.com/opencv/opencv/master/data/haarcascades/haarcascade_frontalface_default.xml
   ```
3. Build the project:
   ```
   mvn clean install
   ```
4. Run the application:
   ```
   mvn exec:java
   ```

## Features
1. Face Recognition:
   - Detects faces in real-time
   - Recognizes known faces from the database
   - Shows name labels for recognized faces
   
2. Database Management:
   - Add new faces with associated names
   - Automatically stores face data in SQLite database
   - Loads existing faces for recognition

## Usage
When you run the application, you'll see a menu with these options:
1. Recognize Face - Start face recognition mode
2. Add New Face - Add a new face to the database
3. Exit - Close the application

## Note
- Make sure your webcam is connected and working
- Good lighting conditions improve recognition accuracy
- Look directly at the camera when adding a new face
