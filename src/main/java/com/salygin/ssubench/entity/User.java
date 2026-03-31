package com.salygin.ssubench.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    UUID id;
    String email;
    String password;
    String role;
    Integer balance;
    Boolean blocked;
}
