package com.j.domain.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
public class TagRequest {
    private String user;
    private List<String> add;
    private List<String> remove;
    private String timestamp;
}
