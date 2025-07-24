package com.streaming.example.exception;

public class VideoNotFoundException extends RuntimeException{

    public VideoNotFoundException(String message) {
        super(message);
    }
}
