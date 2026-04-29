package com.claims.mvp.exception;


public class DuplicateUserException extends RuntimeException{
    public DuplicateUserException(String message) {
        super(message);
    }

}
