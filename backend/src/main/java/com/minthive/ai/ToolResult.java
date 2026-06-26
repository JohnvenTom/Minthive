package com.minthive.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class ToolResult {
    private boolean success;
    private String toolName;
    private String toolDisplayName;
    private Map<String, Object> data;
    private String dataSummary;
    private List<NavigationAction> navigation;

    public static ToolResult failure(String toolName, String reason) {
        return ToolResult.builder()
                .success(false)
                .toolName(toolName)
                .dataSummary(reason)
                .build();
    }
}
