package com.acr.review.engine;

import java.io.IOException;
import java.time.Clock;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.acr.review.engine.config.ReviewEngineProperties;

/** OCR CLI 可用性探针；成功与失败结果均短时缓存，探测异常不影响调用方。 */
@Service
public class OcrEngineAvailabilityService
{
    static final int PROBE_TIMEOUT_SECONDS = 5;
    static final int PROBE_MAX_OUTPUT_BYTES = 16_384;
    static final long CACHE_MILLIS = 60_000L;

    private final ExternalProcessRunner processRunner;
    private final ReviewEngineProperties properties;
    private final Clock clock;
    private OcrEngineAvailability cached;
    private long cachedAtMillis;

    @Autowired
    public OcrEngineAvailabilityService(ExternalProcessRunner processRunner, ReviewEngineProperties properties)
    {
        this(processRunner, properties, Clock.systemUTC());
    }

    OcrEngineAvailabilityService(ExternalProcessRunner processRunner, ReviewEngineProperties properties, Clock clock)
    {
        this.processRunner = processRunner;
        this.properties = properties;
        this.clock = clock;
    }

    public synchronized OcrEngineAvailability probe()
    {
        long nowMillis = clock.millis();
        if (cached != null && nowMillis - cachedAtMillis < CACHE_MILLIS)
        {
            return cached;
        }

        cached = executeProbe(new Date(nowMillis));
        cachedAtMillis = nowMillis;
        return cached;
    }

    private OcrEngineAvailability executeProbe(Date detectedAt)
    {
        String executable = effectiveExecutable();
        try
        {
            ExternalProcessRunner.ProcessExecution execution = processRunner.execute(
                List.of(executable, "version"), null, null,
                PROBE_TIMEOUT_SECONDS, PROBE_MAX_OUTPUT_BYTES);
            if (execution.timedOut())
            {
                return unavailable(executable, "OCR 引擎版本探测超时（5 秒）", detectedAt);
            }
            if (execution.exitCode() != 0)
            {
                String detail = summarize(execution.combinedOutput());
                String message = "OCR 引擎版本探测失败（退出码 " + execution.exitCode() + "）";
                if (detail != null)
                {
                    message += "：" + detail;
                }
                return unavailable(executable, message, detectedAt);
            }

            String version = summarize(execution.combinedOutput());
            return new OcrEngineAvailability(true, executable, version,
                version == null ? "OCR 引擎可用" : "OCR 引擎可用：" + version, detectedAt);
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            return unavailable(executable, "OCR 引擎版本探测被中断", detectedAt);
        }
        catch (IOException ex)
        {
            ReviewEngineFailureType failureType = ReviewEngineProcessRunner.classifyStartupFailure(ex);
            return unavailable(executable,
                ReviewEngineProcessRunner.describeStartupFailure(failureType, executable), detectedAt);
        }
        catch (RuntimeException ex)
        {
            return unavailable(executable, "OCR 引擎版本探测异常，请检查 ACR_OCR_EXECUTABLE 配置", detectedAt);
        }
    }

    private String effectiveExecutable()
    {
        String executable = properties.getExecutablePath();
        return executable == null || executable.isBlank() ? "ocr" : executable.trim();
    }

    private static OcrEngineAvailability unavailable(String executable, String message, Date detectedAt)
    {
        return new OcrEngineAvailability(false, executable, null, message, detectedAt);
    }

    private static String summarize(String output)
    {
        if (output == null || output.isBlank())
        {
            return null;
        }
        String value = AnsiTextCleaner.strip(output).trim().replaceAll("\\s+", " ");
        return value.length() <= 240 ? value : value.substring(0, 240);
    }
}
