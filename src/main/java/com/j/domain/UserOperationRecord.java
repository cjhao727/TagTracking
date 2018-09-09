package com.j.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserOperationRecord {
    private String timestamp;
    private List<String> addOperation;
    private List<String> removeOperation;
}
