package org.clematis.storage.web;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Anton Troshin
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String message;
    private HttpStatus code;
}
