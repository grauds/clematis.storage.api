package org.clematis.storage.web;

import org.springframework.http.HttpStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard error wrapper returned by controller endpoints on failure.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Schema(name = "ErrorResponse", description = "Standard error wrapper")
public class ErrorResponse {

    @Schema(description = "Human readable message", example = "File not found")
    private String message;

    @Schema(description = "HTTP status name", example = "NOT_FOUND")
    private String status;

    public ErrorResponse(String message, HttpStatus status) {
        this.message = message;
        this.status = status == null ? null : status.name();
    }

    public ErrorResponse setMessage(String message) {
        this.message = message;
        return this;
    }

    public ErrorResponse setStatus(String status) {
        this.status = status;
        return this;
    }
}