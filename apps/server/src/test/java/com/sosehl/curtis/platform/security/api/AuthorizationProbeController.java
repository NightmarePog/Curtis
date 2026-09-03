package com.sosehl.curtis.platform.security.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Hidden
class AuthorizationProbeController {

    @GetMapping("/v1/admin/probe")
    String read() {
        return "ok";
    }

    @PostMapping("/v1/admin/probe")
    String write() {
        return "ok";
    }

    @PostMapping("/v1/admin/probe/validate")
    String validate(@Valid @RequestBody ProbeRequest request) {
        return request.name();
    }

    @PostMapping(
        value = "/v1/admin/probe/upload",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    String upload(@RequestPart("file") MultipartFile file) {
        return file.getOriginalFilename();
    }

    record ProbeRequest(
        @NotBlank String name,
        List<@NotNull String> values
    ) {}
}
