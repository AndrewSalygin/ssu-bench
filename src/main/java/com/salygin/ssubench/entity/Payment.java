package com.salygin.ssubench.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    UUID id;
    Long fromUser;
    Long toUser;
    Integer amount;
    LocalDateTime createdAt;
}