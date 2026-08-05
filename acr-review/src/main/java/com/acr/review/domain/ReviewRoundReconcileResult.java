package com.acr.review.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 一轮 SUCCESS 对账结果：新物化 / 转待复核 / 自动复活。 */
public final class ReviewRoundReconcileResult
{
    private static final ReviewRoundReconcileResult EMPTY =
        new ReviewRoundReconcileResult(List.of(), List.of(), List.of());

    private final List<ReviewIssue> newlyMaterialized;
    private final List<ReviewIssue> movedToRechecking;
    private final List<ReviewIssue> reopened;

    public ReviewRoundReconcileResult(List<ReviewIssue> newlyMaterialized,
                                      List<ReviewIssue> movedToRechecking,
                                      List<ReviewIssue> reopened)
    {
        this.newlyMaterialized = copy(newlyMaterialized);
        this.movedToRechecking = copy(movedToRechecking);
        this.reopened = copy(reopened);
    }

    public static ReviewRoundReconcileResult empty()
    {
        return EMPTY;
    }

    /** 由台账 RECHECKING 标题派生（处置重渲染 / 人工重试路径）。 */
    public static ReviewRoundReconcileResult forRecheckingTitles(List<String> titles)
    {
        if (titles == null || titles.isEmpty())
        {
            return EMPTY;
        }
        List<ReviewIssue> moved = new ArrayList<>(titles.size());
        for (String title : titles)
        {
            ReviewIssue issue = new ReviewIssue();
            issue.setTitle(title);
            issue.setStatus(ReviewIssueConstants.STATUS_RECHECKING);
            moved.add(issue);
        }
        return new ReviewRoundReconcileResult(List.of(), moved, List.of());
    }

    public List<ReviewIssue> getNewlyMaterialized()
    {
        return newlyMaterialized;
    }

    public List<ReviewIssue> getMovedToRechecking()
    {
        return movedToRechecking;
    }

    public List<ReviewIssue> getReopened()
    {
        return reopened;
    }

    /** 转待复核问题标题（装配「疑似已修复」段用）。 */
    public List<String> recheckingTitles()
    {
        if (movedToRechecking.isEmpty())
        {
            return List.of();
        }
        List<String> titles = new ArrayList<>(movedToRechecking.size());
        for (ReviewIssue issue : movedToRechecking)
        {
            if (issue != null && issue.getTitle() != null && !issue.getTitle().isBlank())
            {
                titles.add(issue.getTitle());
            }
            else
            {
                titles.add(ReviewIssueConstants.DEFAULT_TITLE);
            }
        }
        return titles;
    }

    public boolean hasRechecking()
    {
        return !movedToRechecking.isEmpty();
    }

    private static List<ReviewIssue> copy(List<ReviewIssue> source)
    {
        if (source == null || source.isEmpty())
        {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(source));
    }
}
