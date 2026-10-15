package com.ecommerce.common.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ApiResponse<T> {

    private String status;
    private String message;
    private T body;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> ApiResponse<T> success(
            String message,
            T body
    ) {
        return ApiResponse.<T>builder()
                .status("success")
                .message(message)
                .body(body)
                .build();
    }

    public static ApiResponse<Void> error(String message) {
        return ApiResponse.<Void>builder()
                .status("error")
                .message(message)
                .body(null)
                .build();
    }
}


