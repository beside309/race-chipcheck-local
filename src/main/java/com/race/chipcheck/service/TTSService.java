package com.race.chipcheck.service;

import com.race.chipcheck.model.Athlete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 文本转语音服务（TTS）
 * 使用 Windows 系统 TTS API（通过 PowerShell 调用）
 */
public class TTSService {
    private static final Logger logger = LoggerFactory.getLogger(TTSService.class);
    private final ExecutorService executor;  // 单线程池，保证播报顺序
    private final PreferenceService preferenceService;

    public TTSService(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * 播报文本（异步）
     * @param text 要播报的文本
     */
    public void speak(String text) {
        if (!preferenceService.isVoiceEnabled()) {
            logger.info("语音播报已禁用，跳过播放");
            return;  // 开关关闭，直接返回
        }

        logger.info("开始播报：{}", text);
        executor.submit(() -> {
            try {
                // PowerShell 脚本调用系统 TTS
                String script = String.format(
                    "Add-Type -AssemblyName System.Speech; " +
                    "$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                    "$speak.Speak('%s')",
                    escapeForPowerShell(text)
                );

                ProcessBuilder pb = new ProcessBuilder(
                    "powershell",
                    "-NoProfile",           // 跳过配置文件加载
                    "-NonInteractive",      // 非交互模式
                    "-ExecutionPolicy", "Bypass",
                    "-Command", script
                );
                pb.redirectErrorStream(true);
                Process process = pb.start();

                // 等待完成（超时 10 秒）
                boolean finished = process.waitFor(10, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    logger.warn("TTS 超时，已强制停止");
                } else {
                    logger.info("TTS 播报完成：{}", text);
                }

            } catch (Exception e) {
                logger.error("TTS 播放失败", e);
            }
        });
    }

    /**
     * 播报选手信息（根据配置：参赛号码、姓名可多选，都选中时播报“参赛号码+姓名”）
     * @param athlete 选手信息
     */
    public void speakAthleteInfo(Athlete athlete) {
        boolean speakBib = preferenceService.isVoiceContentBibEnabled();
        boolean speakName = preferenceService.isVoiceContentNameEnabled();

        if (!speakBib && !speakName) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        if (speakBib) {
            sb.append(formatBibNumber(athlete.getBibNumber()));
        }
        if (speakName) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(athlete.getName() != null ? athlete.getName() : "");
        }
        speak(sb.toString());
    }

    /**
     * 格式化号码布为逐字形式（提高识别度）
     * @param bibNumber 号码布
     * @return 逐字形式（空格分隔）
     */
    private String formatBibNumber(String bibNumber) {
        return bibNumber.chars()
            .mapToObj(c -> String.valueOf((char) c))
            .collect(Collectors.joining(" "));
    }

    /**
     * 转义 PowerShell 特殊字符
     * @param text 原始文本
     * @return 转义后的文本
     */
    private String escapeForPowerShell(String text) {
        return text.replace("'", "''")
                   .replace("\"", "`\"")
                   .replace("$", "`$")
                   .replace("`", "``");
    }

    /**
     * 预热 TTS 系统（在后台加载 PowerShell 和 System.Speech）
     * 应在应用启动时调用，避免首次使用时的延迟
     */
    public void warmup() {
        logger.info("开始预热 TTS 系统");
        executor.submit(() -> {
            try {
                // 播报一个真实的号码，预加载中文 TTS 引擎
                String script = "Add-Type -AssemblyName System.Speech; " +
                               "$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                               "$speak.Volume = 1; " +  // 极低音量（1/100）
                               "$speak.Speak('0')";     // 播报 "0" 预加载

                ProcessBuilder pb = new ProcessBuilder(
                    "powershell",
                    "-NoProfile",
                    "-NonInteractive",
                    "-ExecutionPolicy", "Bypass",
                    "-Command", script
                );
                pb.redirectErrorStream(true);
                Process process = pb.start();

                boolean finished = process.waitFor(15, TimeUnit.SECONDS);
                if (finished) {
                    logger.info("TTS 系统预热完成");
                } else {
                    process.destroyForcibly();
                    logger.warn("TTS 预热超时");
                }
            } catch (Exception e) {
                logger.error("TTS 预热失败", e);
            }
        });
    }

    /**
     * 关闭服务
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
