package com.minthive.runner;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 动态 Banner 颜色播放器
 *
 * <p>功能描述：应用启动完成后，在控制台动态刷新 banner 颜色，实现彩虹流动动画效果</p>
 * <p>原理：使用 ANSI 转义码将光标上移到 banner 位置，以不同颜色方案重绘 banner 文字；
 * Windows 下通过 JNA 调用 kernel32.dll 的 SetConsoleMode 启用虚拟终端处理</p>
 * <p>注意事项：运行在守护线程中，不会阻塞应用关闭</p>
 */
@Slf4j
@Component
public class DynamicBannerRunner implements ApplicationRunner {

    /** Banner 原始文字行（不含颜色码） */
    private static final List<String> BANNER_LINES = List.of(
            "███╗   ███╗██╗███╗   ██╗████████╗██╗  ██╗██╗██╗   ██╗███████╗",
            "████╗ ████║██║████╗  ██║╚══██╔══╝██║  ██║██║██║   ██║██╔════╝",
            "██╔████╔██║██║██╔██╗ ██║   ██║   ███████║██║██║   ██║█████╗",
            "██║╚██╔╝██║██║██║╚██╗██║   ██║   ██╔══██║██║╚██╗ ██╔╝██╔══╝",
            "██║ ╚═╝ ██║██║██║ ╚████║   ██║   ██║  ██║██║ ╚████╔╝ ███████╗",
            "╚═╝     ╚═╝╚═╝╚═╝  ╚═══╝   ╚═╝   ╚═╝  ╚═╝╚═╝  ╚═══╝  ╚══════╝"
    );

    /** ANSI 颜色码序列（红/黄/绿/青/蓝/品红） */
    private static final String[] ANSI_COLORS = {
            "\033[91m",  // BRIGHT_RED
            "\033[93m",  // BRIGHT_YELLOW
            "\033[92m",  // BRIGHT_GREEN
            "\033[96m",  // BRIGHT_CYAN
            "\033[94m",  // BRIGHT_BLUE
            "\033[95m"   // BRIGHT_MAGENTA
    };

    /** ANSI 重置码 */
    private static final String ANSI_RESET = "\033[0m";

    /** 颜色数量 */
    private static final int NUM_COLORS = ANSI_COLORS.length;

    /** 颜色偏移量（每行偏移2个色段，形成斜向彩虹） */
    private static final int ROW_SHIFT = 2;

    /** 横向滚动：每种颜色的字符宽度 */
    private static final int COLOR_BLOCK_WIDTH = 5;

    /** 横向滚动：一个彩虹周期的字符宽度（6色×2格=12） */
    private static final int CYCLE_WIDTH = NUM_COLORS * COLOR_BLOCK_WIDTH;

    /** 动画帧间隔（毫秒） */
    private static final long FRAME_INTERVAL_MS = 100;

    /** 动画持续时长（秒） */
    private static final long ANIMATION_DURATION_SEC = 20;

    /** Windows kernel32.dll 中 ENABLE_VIRTUAL_TERMINAL_PROCESSING 标志值 */
    private static final int ENABLE_VIRTUAL_TERMINAL_PROCESSING = 0x0004;

    /** Windows STD_OUTPUT_HANDLE 常量 */
    private static final int STD_OUTPUT_HANDLE = -11;

    /** JNA 加载的 kernel32 实例 */
    private static final Kernel32 KERNEL32;

    static {
        Kernel32 instance = null;
        try {
            instance = Native.load("kernel32", Kernel32.class);
        } catch (Throwable ignored) {
            // 非 Windows 或 JNA 不可用
        }
        KERNEL32 = instance;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean ansiReady = enableWindowsAnsi();

        if (!ansiReady) {
            log.debug("终端不支持 ANSI 颜色，跳过动态 banner 动画");
            return;
        }

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "dynamic-banner");
            t.setDaemon(true);
            return t;
        });

        // 延迟1秒启动，确保 Spring Boot 启动日志输出完毕
        scheduler.schedule(() -> startAnimation(scheduler), 1, TimeUnit.SECONDS);
    }

    /**
     * 启动颜色流动动画
     *
     * @param scheduler 定时调度器
     */
    private void startAnimation(ScheduledExecutorService scheduler) {
        final int[] frame = {0};
        final long startTime = System.currentTimeMillis();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed > ANIMATION_DURATION_SEC * 1000) {
                    drawFrame(0);
                    System.out.flush();
                    scheduler.shutdown();
                    return;
                }

                drawFrame(frame[0]);
                System.out.flush();
                frame[0] = (frame[0] + 1) % NUM_COLORS;
            } catch (Exception e) {
                scheduler.shutdown();
            }
        }, 0, FRAME_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * 绘制一帧 banner（横向彩虹滚动效果）
     *
     * <p>原理：每个字符的颜色由 (字符索引 + 行偏移 - 帧偏移) 决定，
     * 随着帧数递增，颜色整体向右移动，形成彩虹从左向右横向流动的视觉效果。</p>
     *
     * @param colorOffset 帧偏移量（每帧+1，驱动颜色向右滚动）
     */
    private void drawFrame(int colorOffset) {
        // 光标移到 banner 起始位置（banner 6行 + 空行1行 = 7行）
        System.out.print("\033[" + (BANNER_LINES.size() + 1) + "A");

        System.out.println();

        for (int row = 0; row < BANNER_LINES.size(); row++) {
            String line = BANNER_LINES.get(row);
            int lineLen = line.length();
            StringBuilder sb = new StringBuilder();

            int prevCi = -1;
            for (int col = 0; col < lineLen; col++) {
                // 颜色索引：基于字符位置+行偏移-帧偏移，形成横向滚动
                // 除以COLOR_BLOCK_WIDTH使每种颜色占据指定宽度的字符数
                int pos = (col + row * ROW_SHIFT - colorOffset) % CYCLE_WIDTH;
                // 处理负数取模
                if (pos < 0) pos += CYCLE_WIDTH;
                int ci = pos / COLOR_BLOCK_WIDTH;

                // 只在颜色变化时输出颜色码，减少转义序列数量
                if (ci != prevCi) {
                    sb.append(ANSI_COLORS[ci]);
                    prevCi = ci;
                }
                sb.append(line.charAt(col));
            }

            sb.append(ANSI_RESET);
            sb.append("\033[K\n");
            System.out.print(sb);
        }

        System.out.print(ANSI_RESET);
    }

    /**
     * 启用 Windows 终端的 ANSI 虚拟终端模式
     *
     * <p>功能描述：Windows 10+ 的 CMD/PowerShell 默认不启用 ANSI 转义码，
     * 需要通过 Win32 API 的 SetConsoleMode 启用 ENABLE_VIRTUAL_TERMINAL_PROCESSING。</p>
     *
     * <p>实现方式：通过 JNA 直接调用 kernel32.dll 的 GetStdHandle + GetConsoleMode + SetConsoleMode</p>
     *
     * @return true 表示终端已启用 ANSI 支持
     */
    private boolean enableWindowsAnsi() {
        String os = System.getProperty("os.name", "").toLowerCase();

        // 非 Windows 系统默认支持 ANSI
        if (!os.contains("win")) {
            return true;
        }

        // JNA 不可用（理论上不会发生，因为依赖已添加）
        if (KERNEL32 == null) {
            log.warn("JNA kernel32 加载失败，动态 banner 可能无法正常显示颜色");
            return false;
        }

        try {
            // 1. 获取标准输出句柄
            Pointer handle = KERNEL32.GetStdHandle(STD_OUTPUT_HANDLE);
            if (handle == null || handle.equals(Pointer.NULL)) {
                log.warn("无法获取 Windows 标准输出句柄");
                return false;
            }

            // 2. 获取当前控制台模式
            IntByReference mode = new IntByReference();
            boolean getOk = KERNEL32.GetConsoleMode(handle, mode);
            if (!getOk) {
                log.warn("无法获取 Windows 控制台模式");
                return false;
            }

            int currentMode = mode.getValue();

            // 3. 已经启用了虚拟终端处理
            if ((currentMode & ENABLE_VIRTUAL_TERMINAL_PROCESSING) != 0) {
                log.debug("Windows 终端已启用虚拟终端处理");
                return true;
            }

            // 4. 启用虚拟终端处理
            int newMode = currentMode | ENABLE_VIRTUAL_TERMINAL_PROCESSING;
            boolean setOk = KERNEL32.SetConsoleMode(handle, newMode);

            if (setOk) {
                log.info("已启用 Windows 终端 ANSI 虚拟终端模式");
                return true;
            } else {
                log.warn("启用 Windows 虚拟终端模式失败，动态 banner 可能无法正常显示颜色");
                return false;
            }
        } catch (Throwable e) {
            log.warn("Windows ANSI 启用异常: {}，动态 banner 可能无法正常显示", e.getMessage());
            return false;
        }
    }

    /**
     * Windows kernel32.dll 接口定义（JNA）
     */
    interface Kernel32 extends Library {
        /**
         * 获取标准设备句柄
         *
         * @param nStdHandle 标准设备标识（STD_OUTPUT_HANDLE = -11）
         * @return 设备句柄
         */
        Pointer GetStdHandle(int nStdHandle);

        /**
         * 获取控制台模式
         *
         * @param hConsoleHandle 控制台句柄
         * @param lpMode        输出当前模式
         * @return 是否成功
         */
        boolean GetConsoleMode(Pointer hConsoleHandle, IntByReference lpMode);

        /**
         * 设置控制台模式
         *
         * @param hConsoleHandle 控制台句柄
         * @param dwMode        目标模式
         * @return 是否成功
         */
        boolean SetConsoleMode(Pointer hConsoleHandle, int dwMode);
    }
}
