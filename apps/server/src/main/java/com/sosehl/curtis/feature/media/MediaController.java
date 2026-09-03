package com.sosehl.curtis.feature.media;

import com.sosehl.curtis.feature.media.dto.MediaResponse;
import com.sosehl.curtis.platform.security.domain.CurrentUser;
import com.sosehl.curtis.platform.security.web.SOSE_CurrentUser;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@PreAuthorize("isAuthenticated()")
@Tag(name = "Media", description = "Private images used by Curtis quizzes")
public class MediaController {

    private final MediaService mediaService;

    MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping(
        value = { "/v1/teacher/media", "/v1/admin/media" },
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAnyRole('TEACHER','ADMINISTRATOR')")
    @Operation(summary = "Upload a quiz image")
    ResponseEntity<MediaResponse> upload(
        @RequestPart("file") MultipartFile file,
        @SOSE_CurrentUser CurrentUser currentUser
    ) throws IOException {
        StoredMedia stored = mediaService.upload(
            currentUser.id(),
            file.getOriginalFilename(),
            file.getInputStream()
        );
        return ResponseEntity
            .created(URI.create("/v1/media/" + stored.id()))
            .body(toResponse(stored));
    }

    @GetMapping("/v1/media/{mediaId}")
    @Operation(summary = "Read an authorized quiz image")
    ResponseEntity<Resource> read(
        @PathVariable UUID mediaId,
        @SOSE_CurrentUser CurrentUser currentUser
    ) throws IOException {
        StoredMedia stored = mediaService.requireReadable(
            currentUser.id(),
            currentUser.isAdministrator(),
            mediaId
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(stored.contentType()));
        headers.setContentLength(stored.byteSize());
        headers.setContentDisposition(
            ContentDisposition.inline().filename(stored.originalName()).build()
        );
        headers.setCacheControl(CacheControl.noCache());
        return ResponseEntity.ok()
            .headers(headers)
            .body(stored.content());
    }

    private MediaResponse toResponse(StoredMedia stored) {
        return new MediaResponse(
            stored.id(),
            stored.originalName(),
            stored.contentType(),
            stored.byteSize(),
            stored.sha256()
        );
    }
}
