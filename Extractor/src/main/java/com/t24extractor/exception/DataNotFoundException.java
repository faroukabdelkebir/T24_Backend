package com.t24extractor.exception;


import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DataNotFoundException extends Exception{

    public DataNotFoundException(String message) {
        super(message);
    }
}
