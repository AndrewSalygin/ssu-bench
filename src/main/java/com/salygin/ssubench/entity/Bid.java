package com.salygin.ssubench.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bid {
    UUID id;
    UUID taskId;
    UUID executorId;
    String status;
}