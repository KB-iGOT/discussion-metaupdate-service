package com.igot.cb.pores.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class CustomExceptionTest {

    @Test
    void testNoArgsConstructorAndSettersGetters() {
        CustomException exception = new CustomException();

        exception.setCode("ERR001");
        exception.setMessage("Something went wrong");
        exception.setHttpStatusCode(HttpStatus.BAD_REQUEST);

        assertEquals("ERR001", exception.getCode());
        assertEquals("Something went wrong", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatusCode());
    }

    @Test
    void testAllArgsConstructor() {
        CustomException exception = new CustomException(
                "ERR002",
                "Invalid input",
                HttpStatus.NOT_FOUND
        );

        assertEquals("ERR002", exception.getCode());
        assertEquals("Invalid input", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatusCode());
    }
}

