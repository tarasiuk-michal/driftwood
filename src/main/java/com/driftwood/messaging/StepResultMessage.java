package com.driftwood.messaging;

import java.util.UUID;

public record StepResultMessage(UUID instanceId, String stepId, int attemptCount, boolean success, String errorMessage) {}
