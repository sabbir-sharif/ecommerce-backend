package com.ecommerce.common.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class ApiErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
}

// Can use java record for immutable

//public record ApiErrorResponse(
//        LocalDateTime timestamp,
//        int status,
//        String error,
//        String message,
//        String path
//) {
//}
