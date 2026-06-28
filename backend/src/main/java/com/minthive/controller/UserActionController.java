package com.minthive.controller;

import com.minthive.common.Result;
import com.minthive.entity.AiUserLog;
import com.minthive.security.UserContext;
import com.minthive.service.AiUserLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "用户行为接口")
@RestController
@RequestMapping("/api/user/action")
public class UserActionController {

    private final AiUserLogService aiUserLogService;

    public UserActionController(AiUserLogService aiUserLogService) {
        this.aiUserLogService = aiUserLogService;
    }

    @Data
    public static class StayDto {
        private Long postId;
        private Integer duration;
        private String tags;
    }

    @Operation(summary = "上报帖子停留时长")
    @PostMapping("/stay")
    public Result<Void> reportStay(@RequestBody StayDto dto) {
        Long userId = UserContext.getUserId();
        try {
            AiUserLog log = new AiUserLog();
            log.setUserId(userId);
            log.setPostId(dto.getPostId());
            log.setActionType("stay");
            log.setDuration(dto.getDuration());
            log.setInterestSnapshot(dto.getTags());
            log.setActionTime(LocalDateTime.now());
            aiUserLogService.record(log);
        } catch (Exception e) {
            // 行为日志不影响主流程
        }
        return Result.success();
    }
}
