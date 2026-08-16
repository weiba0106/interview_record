package com.interviewrecord.common.error;

/** 一次性下载令牌已过期、已被使用或不存在（对外统一提示，不泄露资源归属）。 */
public class ExportLinkExpiredException extends RuntimeException {
    public ExportLinkExpiredException() {
        super("下载链接已失效或已被使用");
    }
}
