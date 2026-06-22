package com.driftwood.api;

import com.driftwood.events.WorkflowEventBus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class SseController {

    private final WorkflowEventBus eventBus;

    @GetMapping("/events/stream")
    public SseEmitter stream() {
        return eventBus.subscribe();
    }
}
