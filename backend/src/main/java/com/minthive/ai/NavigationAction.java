package com.minthive.ai;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NavigationAction {
    private String label;
    private String path;
    private String type;
    private String preview;
}
