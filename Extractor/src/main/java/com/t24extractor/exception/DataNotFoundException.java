package com.t24extractor.exception;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@RequiredArgsConstructor
@ResponseStatus(HttpStatus.NOT_FOUND)
public class DataNotFoundException extends Exception{

    public DataNotFoundException(String message) {
        super(message);
    }
}
