package com.acr.review.scheduling;

/** 已获取的审查资源名额；关闭时统一释放，避免散落在执行流程各处。 */
public final class ReviewBudgetLease implements AutoCloseable
{
    private final ReviewResourceBudgetService budgetService;
    private final boolean workspace;
    private final boolean ocr;
    private final boolean llm;
    private boolean closed;

    ReviewBudgetLease(ReviewResourceBudgetService budgetService,
                      boolean workspace,
                      boolean ocr,
                      boolean llm)
    {
        this.budgetService = budgetService;
        this.workspace = workspace;
        this.ocr = ocr;
        this.llm = llm;
    }

    @Override
    public void close()
    {
        if (closed)
        {
            return;
        }
        closed = true;
        budgetService.release(workspace, ocr, llm);
    }
}
