package com.sosehl.curtis.platform.realtime.api;

import io.swagger.v3.oas.annotations.Operation;
import com.sosehl.curtis.platform.realtime.application.LiveEventRegistry;
import com.sosehl.curtis.platform.security.domain.CurrentUser;
import com.sosehl.curtis.platform.security.web.SOSE_CurrentUser;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/v1/events")
public class LiveEventController {
    private final LiveEventRegistry registry;

    public LiveEventController(LiveEventRegistry registry) {
        this.registry = registry;
    }

    @Operation(hidden = true)
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> subscribe(
        @SOSE_CurrentUser CurrentUser user
    ) {
        SseEmitter emitter = registry.subscribe(
            user.id(),
            user.identityKey()
        );
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .header("X-Accel-Buffering", "no")
            .body(emitter);
    }
}
