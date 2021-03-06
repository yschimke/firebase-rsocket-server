package com.github.mostroverkhov.firebase_rsocket.internal.handler.read.cache.firebase;

import com.github.mostroverkhov.firebase_rsocket.servercommon.model.read.ReadRequest;

import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * Author: mostroverkhov
 */
public class CacheDurationConstant implements CacheDuration {
    private final long cacheDurationSeconds;

    public CacheDurationConstant(int units, TimeUnit timeUnit) {
        cacheDurationSeconds = timeUnit.toSeconds(units);
    }

    @Override
    public long getDuration() {
        return cacheDurationSeconds;
    }

    @Override
    public void readRequest(ReadRequest request) {
        /**/
    }
}
