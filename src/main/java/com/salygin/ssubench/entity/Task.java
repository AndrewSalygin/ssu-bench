package com.salygin.ssubench.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    UUID id;
    String title;
    String description;
    Integer reward;
    TaskStatus status;
    UUID customerId;
    UUID executorId;
}
