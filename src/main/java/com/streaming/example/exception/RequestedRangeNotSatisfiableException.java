package com.streaming.example.exception;

public class RequestedRangeNotSatisfiableException extends RuntimeException{

    public RequestedRangeNotSatisfiableException(String message) {
        super(message);
    }
}
