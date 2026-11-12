package com.minthive.ai;

import com.minthive.common.Result;
import com.minthive.common.ResultCode;
import com.minthive.controller.AiController;
import com.minthive.service.CircleService;
import com.minthive.service.FollowService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiControllerSecurityTest {
    @Test
    void unavailablePrecheckIsNotSuccessfulModeration() {
        AiContext context = mock(AiContext.class);
        AiRateLimiter limiter = mock(AiRateLimiter.class);
        AiService.AiDetectResult unavailable = AiService.AiDetectResult.unavailable("本地审核未接入");
        when(context.detectContent("text", null)).thenReturn(unavailable);
        AiController controller = new AiController(context, limiter,
                mock(CircleService.class), mock(FollowService.class));
        AiController.DetectDto dto = new AiController.DetectDto();
        dto.setText("text");

        Result<AiService.AiDetectResult> response = controller.detect(dto);

        assertEquals(ResultCode.AI_SERVICE_ERROR.getCode(), response.getCode());
        assertSame(unavailable, response.getData());
        assertFalse(response.getData().available);
        assertTrue(response.getMsg().contains("不可用"));
        verify(limiter).reserve();
    }

    @Test
    void completedModerationKeepsExistingResponseFields() {
        AiContext context = mock(AiContext.class);
        AiService.AiDetectResult safe = new AiService.AiDetectResult(false, "无", "low");
        when(context.detectContent("text", null)).thenReturn(safe);
        AiController controller = new AiController(context, mock(AiRateLimiter.class),
                mock(CircleService.class), mock(FollowService.class));
        AiController.DetectDto dto = new AiController.DetectDto();
        dto.setText("text");
        var response = controller.detect(dto);
        assertEquals(ResultCode.SUCCESS.getCode(), response.getCode());
        assertSame(safe, response.getData());
        assertTrue(safe.available);
    }
}
