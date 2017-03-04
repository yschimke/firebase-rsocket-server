package com.github.mostroverkhov.firebase_rsocket.auth;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Notification;

/**
 * Created by Maksym Ostroverkhov on 28.02.17.
 */
public class ServerAuthenticator implements Authenticator {

    private final Flowable<Credentials> credsFlow;

    public ServerAuthenticator(CredentialsFactory credsFlow) {
        this.credsFlow = credsFlow.getCreds().toFlowable()
                .materialize()
                .filter(Notification::isOnNext)
                .cache()
                .dematerialize();
    }

    @Override
    public Completable authenticate() {
        return credsFlow
                .map(credentials -> new FirebaseServerAuth(
                        credentials.getServiceFile(),
                        credentials.getDbUrl(),
                        credentials.getUserId()))
                .flatMapCompletable(FirebaseServerAuth::authenticate);
    }
}