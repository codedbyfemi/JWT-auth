package com.example.testapi.model;

import jakarta.validation.constraints.NotBlank;

public record Greet (@NotBlank String message){ }
