package com.j.domain.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter @Setter
@NoArgsConstructor
public class TagRequest {
    private String user;
    private Set<String> add;
    private Set<String> remove;
    private String timestamp;
}
