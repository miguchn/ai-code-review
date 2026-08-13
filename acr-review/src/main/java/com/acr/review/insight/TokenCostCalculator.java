package com.acr.review.insight;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 按当前单价估算成本；缺单价或缺用量一律返回 null，禁止用 0 冒充。 */
public final class TokenCostCalculator
{
    private static final BigDecimal THOUSAND = new BigDecimal("1000");
    private static final int SCALE = 4;

    private TokenCostCalculator()
    {
    }

    public static BigDecimal estimate(Integer inputTokens, Integer outputTokens,
                                      BigDecimal inputPricePer1k, BigDecimal outputPricePer1k)
    {
        if (inputTokens == null && outputTokens == null)
        {
            return null;
        }
        if (inputPricePer1k == null || outputPricePer1k == null)
        {
            return null;
        }
        BigDecimal input = BigDecimal.valueOf(inputTokens == null ? 0 : inputTokens);
        BigDecimal output = BigDecimal.valueOf(outputTokens == null ? 0 : outputTokens);
        return input.divide(THOUSAND, 8, RoundingMode.HALF_UP).multiply(inputPricePer1k)
            .add(output.divide(THOUSAND, 8, RoundingMode.HALF_UP).multiply(outputPricePer1k))
            .setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static Double toDouble(BigDecimal value)
    {
        return value == null ? null : value.doubleValue();
    }
}
