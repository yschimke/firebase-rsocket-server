package com.github.mostroverkhov.firebase_rsocket.internal.handler.read;

import com.github.mostroverkhov.datawindowsource.model.DataItem;
import com.github.mostroverkhov.datawindowsource.model.DataQuery;
import com.github.mostroverkhov.datawindowsource.model.NextQuery;
import com.github.mostroverkhov.datawindowsource.model.WindowChangeEvent;
import com.github.mostroverkhov.firebase_data_rxjava.rx.FirebaseDatabaseManager;
import com.github.mostroverkhov.firebase_rsocket.servercommon.KeyValue;
import com.github.mostroverkhov.firebase_rsocket.servercommon.model.notifications.NotifEventKind;
import com.github.mostroverkhov.firebase_rsocket.servercommon.model.notifications.TypedNotifResponse;
import com.github.mostroverkhov.firebase_rsocket.servercommon.model.read.ReadRequest;
import com.github.mostroverkhov.firebase_rsocket.internal.handler.read.cache.firebase.CacheDuration;
import com.google.firebase.database.DatabaseReference;
import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.Flowable;
import rx.Observable;

import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * Author: mostroverkhov
 */
public class NotifHandler extends BaseDataWindowHandler<TypedNotifResponse> {

    public NotifHandler(String key, String value) {
        super(key, value);
    }

    @Override
    public Flowable<TypedNotifResponse> handle(KeyValue metadata, ReadRequest request) {
        DataQuery dataQuery = toDataQuery(request);
        DatabaseReference dbRef = dataQuery.getDbRef();

        tryCache(request, dbRef);

        Observable<DataItem> notifications = new FirebaseDatabaseManager(dbRef)
                .data()
                .notifications(dataQuery);
        Flowable<TypedNotifResponse> dataWindowNotifFlow =
                RxJavaInterop.toV2Flowable(notifications)
                        .map(dataItem -> toNotif(request, dataItem));
        return dataWindowNotifFlow;
    }

    private TypedNotifResponse toNotif(ReadRequest curRequest, DataItem dataItem) {
        if (dataItem instanceof NextQuery) {
            NextQuery nextQuery = (NextQuery) dataItem;
            ReadRequest nextReadRequest = nextReadRequest(curRequest, nextQuery.getNext());
            return TypedNotifResponse.nextWindow(nextReadRequest);
        } else if (dataItem instanceof WindowChangeEvent) {
            WindowChangeEvent changeEvent = (WindowChangeEvent) dataItem;
            NotifEventKind eventKind = toKind(changeEvent.getKind());
            return TypedNotifResponse.changeEvent(eventKind, changeEvent.getItem());
        } else {
            throw unknownType(dataItem);
        }
    }

    private NotifEventKind toKind(WindowChangeEvent.Kind kind) {
        switch (kind) {
            case ADDED:
                return NotifEventKind.ADDED;
            case MOVED:
                return NotifEventKind.MOVED;
            case CHANGED:
                return NotifEventKind.CHANGED;
            case REMOVED:
                return NotifEventKind.REMOVED;
            default:
                throw new AssertionError("Unknown notification event kind: " + kind);
        }
    }

    private IllegalArgumentException unknownType(DataItem dataItem) {
        return new IllegalArgumentException("Unknown data window notification type: "
                + dataItem.getClass().getName());
    }

    @SuppressWarnings("Duplicates")
    private void tryCache(ReadRequest readRequest, DatabaseReference dbRef) {
        cache.ifPresent(c -> {
            CacheDuration dur = c.cacheDuration();
            dur.readRequest(readRequest);
            long duration = dur.getDuration();
            c.nativeCache().cache(dbRef, duration, TimeUnit.SECONDS);
        });
    }
}
