package org.clematis.storage.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestResponse {
    private String fileName;
    private String downloadUrl;
    private String fileType;
    private long fileSize;
}
