package com.acr.review.scope;

/** Diff 右侧（head 版本）的行号区间，闭区间。 */
public record LineRange(int start, int end)
{
    public LineRange
    {
        if (start < 1 || end < start)
        {
            throw new IllegalArgumentException("非法行号区间：" + start + "-" + end);
        }
    }

    public boolean intersects(LineRange other)
    {
        return other != null && this.start <= other.end && other.start <= this.end;
    }

    public boolean contains(int line)
    {
        return line >= start && line <= end;
    }

    /** 与另一区间的最近距离；相交时为 0。 */
    public int distanceTo(LineRange other)
    {
        if (intersects(other))
        {
            return 0;
        }
        return other.start > this.end ? other.start - this.end : this.start - other.end;
    }
}
