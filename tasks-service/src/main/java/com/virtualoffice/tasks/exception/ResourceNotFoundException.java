package com.virtualoffice.tasks.exception;

/** 404 — the requested task does not exist. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
