package com.likith.AI.Code.Intelligence.SaaS.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AskResponse {

    private String answer;
    private boolean showSnippets;

}