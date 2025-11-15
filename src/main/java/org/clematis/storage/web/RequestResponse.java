package org.clematis.storage.web;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * DTO returned after a successful upload or in file listing responses.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Schema(name = "RequestResponse", description = "Response returned after a successful upload")
public class RequestResponse {

    @Schema(description = "Original filename (or logical stored filename)", example = "photo.png")
    private String fileName;

    @Schema(
        description = "Public URL that can be used to download the file",
        example = "http://localhost:8080/api/files/download/1234"
    )
    private String downloadUrl;

    @Schema(description = "MIME type of the file", example = "image/png")
    private String contentType;

    @Schema(description = "File size in bytes", example = "34567")
    private long size;

    public RequestResponse setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public RequestResponse setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
        return this;
    }

    public RequestResponse setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public RequestResponse setSize(long size) {
        this.size = size;
        return this;
    }
}
