package com.acr.common.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI-Code-Review 运行时可调参数（见 application.yml 中 acr.*）
 */
@ConfigurationProperties(prefix = "acr")
public class AcrRuntimeProperties
{
    private final Ai ai = new Ai();

    public Ai getAi()
    {
        return ai;
    }

    public static class Ai
    {
        /** 语义推荐时向量分段扫描批大小 */
        private int embeddingRankBatchSize = 512;

        /** 生成查询向量时文本截断上限（字符） */
        private int embeddingTextMaxChars = 16000;

        public int getEmbeddingRankBatchSize()
        {
            return embeddingRankBatchSize;
        }

        public void setEmbeddingRankBatchSize(int embeddingRankBatchSize)
        {
            this.embeddingRankBatchSize = embeddingRankBatchSize;
        }

        public int getEmbeddingTextMaxChars()
        {
            return embeddingTextMaxChars;
        }

        public void setEmbeddingTextMaxChars(int embeddingTextMaxChars)
        {
            this.embeddingTextMaxChars = embeddingTextMaxChars;
        }
    }
}
