package com.windsurfproject;

public class FaceData {
    private int id;
    private String name;
    private byte[] faceData;

    public FaceData(int id, String name, byte[] faceData) {
        this.id = id;
        this.name = name;
        this.faceData = faceData;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public byte[] getFaceData() {
        return faceData;
    }
}
