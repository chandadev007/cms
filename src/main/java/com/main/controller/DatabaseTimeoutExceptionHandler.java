package com.main.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class DatabaseTimeoutExceptionHandler {
  private static final Logger logger = LoggerFactory.getLogger(DatabaseTimeoutExceptionHandler.class);

  // 1. Handles CONNECT_TIMEOUT (Database is down or network link is broken)
  @ExceptionHandler(CannotGetJdbcConnectionException.class)
  public ResponseEntity<Object> handleConnectionTimeout(CannotGetJdbcConnectionException ex) {
    Map<String, Object> body = new HashMap<>();
    body.put("error", "1");
    body.put("errorDetail",
        "Service Unavailable: database connection could not be established. Please try again later.");

    logger.error("Service Unavailable exception caught: ", ex);

    return new ResponseEntity<>(body, HttpStatus.OK);
  }

  // 2. Handles ReadTimeout (The query executed but took too long to return data)
  @ExceptionHandler(QueryTimeoutException.class)
  public ResponseEntity<Object> handleQueryTimeout(QueryTimeoutException ex) {
    Map<String, Object> body = new HashMap<>();
    body.put("error", "1");
    body.put("errorDetail",
        "Database read query timeout: the database query took too long to respond. The operation was aborted.");

    logger.error("Database read query timeout exception caught: ", ex);

    return new ResponseEntity<>(body, HttpStatus.OK);
  }
}