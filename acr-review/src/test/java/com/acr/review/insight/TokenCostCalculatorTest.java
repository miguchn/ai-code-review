package com.acr.review.insight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TokenCostCalculatorTest
{
    @Test
    void estimatesByCurrentUnitPrice()
    {
        BigDecimal cost = TokenCostCalculator.estimate(2000, 500,
            new BigDecimal("0.0020"), new BigDecimal("0.0080"));
        assertEquals(new BigDecimal("0.0080"), cost);
    }

    @Test
    void returnsNullWhenEitherPriceMissing()
    {
        assertNull(TokenCostCalculator.estimate(100, 20, new BigDecimal("0.01"), null));
        assertNull(TokenCostCalculator.estimate(100, 20, null, new BigDecimal("0.01")));
        assertNull(TokenCostCalculator.estimate(100, 20, null, null));
    }

    @Test
    void returnsNullWhenBothTokenSidesMissing()
    {
        assertNull(TokenCostCalculator.estimate(null, null, new BigDecimal("0.01"), new BigDecimal("0.02")));
    }

    @Test
    void treatsSingleNullTokenSideAsZero()
    {
        BigDecimal cost = TokenCostCalculator.estimate(1000, null,
            new BigDecimal("1.0000"), new BigDecimal("2.0000"));
        assertEquals(new BigDecimal("1.0000"), cost);
    }
}
