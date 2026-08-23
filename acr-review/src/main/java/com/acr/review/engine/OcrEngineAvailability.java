package com.acr.review.engine;

import java.util.Date;

/** OCR CLI 版本探测结果。 */
public record OcrEngineAvailability(boolean available, String executable, String version,
                                    String message, Date detectedAt)
{
}
