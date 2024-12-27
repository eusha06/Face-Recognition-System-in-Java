package com.windsurfproject;

import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.*;
import org.bytedeco.opencv.opencv_objdetect.*;
import org.bytedeco.opencv.opencv_videoio.*;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.indexer.*;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_face.*;
import static org.bytedeco.opencv.global.opencv_videoio.*;
import javax.swing.*;
import java.awt.FlowLayout;
import java.nio.IntBuffer;
import java.io.File;

public class FaceRecognitionApp {
    private final DatabaseHandler db;
    private final CascadeClassifier faceDetector;
    private final LBPHFaceRecognizer recognizer;
    private final VideoCapture capture;
    private final CanvasFrame canvas;
    private final OpenCVFrameConverter.ToMat converterToMat;
    private volatile boolean isRecognizing = false;
    private volatile boolean isAddingFace = false;
    private JFrame controlFrame;

    public FaceRecognitionApp() throws Exception {
        try {
            db = new DatabaseHandler();
            
            // Load cascade classifier
            String cascadePath = new File("src/main/resources/haarcascade_frontalface_default.xml").getAbsolutePath();
            System.out.println("Loading cascade classifier from: " + cascadePath);
            faceDetector = new CascadeClassifier(cascadePath);
            if (faceDetector.isNull()) {
                throw new Exception("Error loading cascade classifier");
            }
            
            recognizer = LBPHFaceRecognizer.create();
            capture = new VideoCapture();
            capture.open(0);
            capture.set(CAP_PROP_FRAME_WIDTH, 640);
            capture.set(CAP_PROP_FRAME_HEIGHT, 480);
            
            if (!capture.isOpened()) {
                throw new Exception("Could not open video capture device");
            }
            
            // Create and show canvas
            canvas = new CanvasFrame("Face Recognition", CanvasFrame.getDefaultGamma());
            canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
            canvas.setVisible(true);
            converterToMat = new OpenCVFrameConverter.ToMat();
            
            // Show first frame to ensure camera is working
            Mat frame = new Mat();
            if (capture.read(frame)) {
                canvas.showImage(converterToMat.convert(frame));
                Thread.sleep(100); // Give time for frame to display
            }
            
            setupControlPanel();
        } catch (Exception e) {
            cleanup();
            throw e;
        }
    }

    private void setupControlPanel() {
        controlFrame = new JFrame("Controls");
        controlFrame.setLayout(new FlowLayout());
        
        JButton recognizeBtn = new JButton("Recognize Face");
        JButton addFaceBtn = new JButton("Add New Face");
        JButton exitBtn = new JButton("Exit");
        
        recognizeBtn.addActionListener(e -> startRecognizing());
        addFaceBtn.addActionListener(e -> startAddingFace());
        exitBtn.addActionListener(e -> {
            cleanup();
            System.exit(0);
        });
        
        controlFrame.add(recognizeBtn);
        controlFrame.add(addFaceBtn);
        controlFrame.add(exitBtn);
        
        controlFrame.setSize(300, 100);
        controlFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        controlFrame.setVisible(true);
    }

    public void run() {
        Mat frame = new Mat();
        Mat grayMat = new Mat();
        RectVector faces = new RectVector();
        
        try {
            long lastFrameTime = System.currentTimeMillis();
            int frameInterval = 33; // Target ~30 FPS
            
            while (!Thread.interrupted() && canvas.isVisible()) {
                try {
                    long currentTime = System.currentTimeMillis();
                    long elapsedTime = currentTime - lastFrameTime;
                    
                    if (elapsedTime < frameInterval) {
                        Thread.sleep(1);
                        continue;
                    }
                    
                    if (capture.read(frame)) {
                        cvtColor(frame, grayMat, COLOR_BGR2GRAY);
                        faceDetector.detectMultiScale(grayMat, faces);
                        
                        // Draw rectangles around detected faces
                        for (int i = 0; i < faces.size(); i++) {
                            Rect face = faces.get(i);
                            rectangle(frame, face, new Scalar(0, 255, 0, 1));
                        }
                        
                        if (isRecognizing && faces.size() > 0) {
                            for (int i = 0; i < faces.size(); i++) {
                                Mat faceROI = null;
                                Mat resizedFace = null;
                                try {
                                    faceROI = new Mat(grayMat, faces.get(i));
                                    resizedFace = new Mat();
                                    resize(faceROI, resizedFace, new Size(200, 200));
                                    
                                    IntPointer label = new IntPointer(1);
                                    DoublePointer confidence = new DoublePointer(1);
                                    try {
                                        recognizer.predict(resizedFace, label, confidence);
                                        
                                        int predictedLabel = label.get(0);
                                        double conf = confidence.get(0);
                                        
                                        String info = db.getUserName(predictedLabel);
                                        String displayText = conf < 100.0 ? info : "Unknown";
                                        putText(frame, displayText, 
                                                new org.bytedeco.opencv.opencv_core.Point(faces.get(i).x(), faces.get(i).y() - 10),
                                                FONT_HERSHEY_SIMPLEX, 1.0, new Scalar(0, 255, 0, 1));
                                    } finally {
                                        if (label != null) label.deallocate();
                                        if (confidence != null) confidence.deallocate();
                                    }
                                } catch (Exception e) {
                                    putText(frame, "Unknown", 
                                            new org.bytedeco.opencv.opencv_core.Point(faces.get(i).x(), faces.get(i).y() - 10),
                                            FONT_HERSHEY_SIMPLEX, 1.0, new Scalar(0, 255, 0, 1));
                                } finally {
                                    if (resizedFace != null) resizedFace.release();
                                    if (faceROI != null) faceROI.release();
                                }
                            }
                        }
                        
                        if (isAddingFace && faces.size() == 1) {
                            String name = JOptionPane.showInputDialog(canvas, "Enter name for the face:");
                            if (name != null && !name.trim().isEmpty()) {
                                String gender = JOptionPane.showInputDialog(canvas, "Enter gender (Male/Female):");
                                if (gender != null && !gender.trim().isEmpty()) {
                                    String ageStr = JOptionPane.showInputDialog(canvas, "Enter age:");
                                    if (ageStr != null && !ageStr.trim().isEmpty()) {
                                        try {
                                            int age = Integer.parseInt(ageStr);
                                            Mat faceROI = null;
                                            Mat resizedFace = null;
                                            try {
                                                faceROI = new Mat(grayMat, faces.get(0));
                                                resizedFace = new Mat();
                                                resize(faceROI, resizedFace, new Size(200, 200));
                                                
                                                // Convert face data to byte array
                                                BytePointer bytePtr = resizedFace.data();
                                                byte[] faceData = new byte[(int)(resizedFace.total() * resizedFace.elemSize())];
                                                bytePtr.get(faceData);
                                                
                                                // Add face to database
                                                int newId = db.addUser(name, gender, age, faceData);
                                                
                                                // Train recognizer with new face
                                                MatVector trainingFaces = new MatVector(1);
                                                Mat labels = new Mat(1, 1, CV_32SC1);
                                                try {
                                                    trainingFaces.put(0, resizedFace);
                                                    labels.ptr(0, 0).putInt(newId);
                                                    recognizer.update(trainingFaces, labels);
                                                    JOptionPane.showMessageDialog(canvas, "Face added successfully!");
                                                } finally {
                                                    labels.release();
                                                    trainingFaces.deallocate();
                                                }
                                            } finally {
                                                if (resizedFace != null) resizedFace.release();
                                                if (faceROI != null) faceROI.release();
                                            }
                                        } catch (NumberFormatException e) {
                                            JOptionPane.showMessageDialog(canvas, "Invalid age entered!");
                                        }
                                    }
                                }
                            }
                            isAddingFace = false;
                        }
                        
                        canvas.showImage(converterToMat.convert(frame));
                        lastFrameTime = currentTime;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } finally {
            if (faces != null) faces.deallocate();
            if (grayMat != null) grayMat.release();
            if (frame != null) frame.release();
        }
    }

    private void startRecognizing() {
        isRecognizing = true;
        isAddingFace = false;
    }
    
    private void startAddingFace() {
        isAddingFace = true;
        isRecognizing = false;
    }
    
    private void cleanup() {
        if (canvas != null) canvas.dispose();
        if (controlFrame != null) controlFrame.dispose();
        if (capture != null) capture.release();
        if (faceDetector != null) faceDetector.close();
        if (recognizer != null) recognizer.close();
    }

    public static void main(String[] args) {
        try {
            FaceRecognitionApp app = new FaceRecognitionApp();
            app.run();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
