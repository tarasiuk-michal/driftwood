package com.driftwood.api;

import com.driftwood.service.OrchestratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/scenarios")
@RequiredArgsConstructor
public class ScenarioController {

    private final OrchestratorService orchestratorService;

    @PostMapping("/{name}")
    public ResponseEntity<Map<String, Object>> run(@PathVariable String name) {
        return switch (name) {
            case "clean-batch" -> {
                List<UUID> ids = IntStream.range(0, 20)
                        .mapToObj(i -> orchestratorService.submit("trivial-workflow", null).id())
                        .toList();
                yield ResponseEntity.accepted().body(Map.of("scenario", name, "instanceIds", ids));
            }
            case "flaky-batch" -> {
                List<UUID> ids = IntStream.range(0, 10)
                        .mapToObj(i -> orchestratorService.submit("flaky-workflow", null).id())
                        .toList();
                yield ResponseEntity.accepted().body(Map.of("scenario", name, "instanceIds", ids));
            }
            case "poison-job" -> {
                UUID id = orchestratorService.submit("poison-workflow", null).id();
                yield ResponseEntity.accepted().body(Map.of("scenario", name, "instanceId", id));
            }
            case "duplicate-test" -> {
                String key = "dup-" + UUID.randomUUID();
                UUID id1 = orchestratorService.submit("trivial-workflow", key).id();
                UUID id2 = orchestratorService.submit("trivial-workflow", key).id();
                yield ResponseEntity.accepted().body(Map.of("scenario", name, "id1", id1, "id2", id2, "deduplicated", id1.equals(id2)));
            }
            default -> ResponseEntity.badRequest().body(Map.of("error", "unknown scenario: " + name));
        };
    }
}
