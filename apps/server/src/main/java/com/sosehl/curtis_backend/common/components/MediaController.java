package com.sosehl.curtis_backend.common.components;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/media")
public class MediaController {

    private final Path mediaRoot;

    public MediaController(
        @Value("${quiz.media.path:./media}") String mediaPath
    ) {
        this.mediaRoot = Paths.get(mediaPath).toAbsolutePath().normalize();
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> get(@PathVariable String filename) {
        Path file = mediaRoot.resolve(filename).normalize();
        if (
            !file.getParent().equals(mediaRoot) ||
            !Files.isRegularFile(file)
        ) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Obrázek nebyl nalezen"
            );
        }

        Resource resource = new FileSystemResource(file);
        MediaType contentType = mediaType(file);
        return ResponseEntity.ok().contentType(contentType).body(resource);
    }

    private MediaType mediaType(Path file) {
        return MediaTypeFactory.getMediaType(file.getFileName().toString())
            .orElse(MediaType.APPLICATION_OCTET_STREAM);
    }
}
