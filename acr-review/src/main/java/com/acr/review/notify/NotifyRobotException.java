package com.acr.review.notify;

/** 群机器人投递失败（不含密钥正文）。 */
public class NotifyRobotException extends RuntimeException
{
    public NotifyRobotException(String message)
    {
        super(message);
    }

    public NotifyRobotException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
