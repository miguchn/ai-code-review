package com.acr.review.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.acr.review.insight.InsightIdentityClaim;

public interface InsightIdentityClaimMapper
{
    List<InsightIdentityClaim> selectByUserId(@Param("userId") Long userId);

    int insertIgnore(InsightIdentityClaim claim);
}
