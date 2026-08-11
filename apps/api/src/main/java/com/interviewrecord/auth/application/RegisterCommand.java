package com.interviewrecord.auth.application;

public record RegisterCommand(String email, String password, String displayName, String timeZone, String clientIp) {}
