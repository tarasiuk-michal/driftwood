package com.driftwood.messaging;

import java.util.UUID;

public record StepDispatchMessage(UUID instanceId, String stepId, int attemptCount) {}
