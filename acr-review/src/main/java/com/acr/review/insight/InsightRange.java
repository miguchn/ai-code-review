package com.acr.review.insight;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import com.acr.common.exception.ServiceException;
import com.acr.common.utils.StringUtils;

/** 看板时间窗口解析。 */
public final class InsightRange
{
    private static final ZoneId ZONE = ZoneId.of(InsightConstants.ZONE_ID);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;

    private final LocalDate begin;
    private final LocalDate end;
    private final LocalDate prevBegin;
    private final LocalDate prevEnd;

    private InsightRange(LocalDate begin, LocalDate end)
    {
        this.begin = begin;
        this.end = end;
        long days = ChronoUnit.DAYS.between(begin, end) + 1;
        this.prevEnd = begin.minusDays(1);
        this.prevBegin = prevEnd.minusDays(days - 1);
    }

    public static InsightRange of(String beginDate, String endDate, Integer days)
    {
        LocalDate end = LocalDate.now(ZONE);
        LocalDate begin;
        if (StringUtils.isNotEmpty(beginDate) && StringUtils.isNotEmpty(endDate))
        {
            begin = LocalDate.parse(beginDate, DAY);
            end = LocalDate.parse(endDate, DAY);
        }
        else
        {
            int window = days == null ? InsightConstants.DEFAULT_RANGE_DAYS : days;
            window = Math.min(Math.max(window, 1), InsightConstants.MAX_RANGE_DAYS);
            begin = end.minusDays(window - 1L);
        }
        if (end.isBefore(begin))
        {
            throw new ServiceException("结束日期不能早于开始日期");
        }
        long span = ChronoUnit.DAYS.between(begin, end) + 1;
        if (span > InsightConstants.MAX_RANGE_DAYS)
        {
            throw new ServiceException("时间范围最多 " + InsightConstants.MAX_RANGE_DAYS + " 天");
        }
        return new InsightRange(begin, end);
    }

    public LocalDate getBegin()
    {
        return begin;
    }

    public LocalDate getEnd()
    {
        return end;
    }

    public LocalDate getPrevBegin()
    {
        return prevBegin;
    }

    public LocalDate getPrevEnd()
    {
        return prevEnd;
    }

    public String beginText()
    {
        return begin.format(DAY);
    }

    public String endText()
    {
        return end.format(DAY);
    }
}
