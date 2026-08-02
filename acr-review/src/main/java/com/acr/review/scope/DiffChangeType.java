package com.acr.review.scope;

/** 文件变更类型。二进制、子模块、仅权限位等以 DiffFileChange 标志位表达，不占类型。 */
public enum DiffChangeType
{
    /** 新增文件（hunk 包含全部行，内容天然完整）。 */
    ADDED,
    /** 已存在文件的内容修改。 */
    MODIFIED,
    /** 删除文件。 */
    DELETED,
    /** 改名（可能同时含内容修改）。 */
    RENAMED
}
