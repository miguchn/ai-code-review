package com.acr.review.git;

/** Git 连接失败分类。 */
public enum GitConnectionFailure
{
    NONE,
    INVALID_REPOSITORY_URL,
    INVALID_CREDENTIAL,
    PERMISSION_DENIED,
    REPOSITORY_NOT_FOUND,
    NETWORK_ERROR,
    API_ERROR,
    TIMEOUT
}
