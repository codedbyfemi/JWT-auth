package com.example.testapi.model;

import java.util.UUID;

public record MeResponse(UUID id, String email, String name) {}
