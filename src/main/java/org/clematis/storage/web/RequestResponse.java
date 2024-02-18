package org.clematis.storage.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Specification of request response data. It carries direct download string to get uploaded file.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestResponse {
    private String fileName;
    private String downloadUrl;
    private String fileType;
    private long fileSize;
}
