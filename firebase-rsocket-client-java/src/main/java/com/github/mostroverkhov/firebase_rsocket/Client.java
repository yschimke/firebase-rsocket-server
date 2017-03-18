package com.github.mostroverkhov.firebase_rsocket;

import com.github.mostroverkhov.firebase_rsocket.gson.GsonUtil;
import com.github.mostroverkhov.firebase_rsocket_data.common.model.read.ReadResponse;
import com.github.mostroverkhov.firebase_rsocket_data.common.model.Op;
import com.github.mostroverkhov.firebase_rsocket_data.common.model.read.ReadRequest;
import com.github.mostroverkhov.firebase_rsocket_data.common.model.delete.DeleteRequest;
import com.github.mostroverkhov.firebase_rsocket_data.common.model.delete.DeleteResponse;
import com.github.mostroverkhov.firebase_rsocket_data.common.model.write.WriteRequest;
import com.github.mostroverkhov.firebase_rsocket_data.common.model.write.WriteResponse;
import com.google.gson.*;
import io.reactivesocket.Frame;
import io.reactivesocket.FrameType;
import io.reactivesocket.Payload;
import io.reactivesocket.ReactiveSocket;
import io.reactivesocket.client.ReactiveSocketClient;
import io.reactivesocket.frame.ByteBufferUtil;
import io.reactivesocket.transport.tcp.client.TcpTransportClient;
import io.reactivesocket.util.PayloadImpl;
import io.reactivex.Flowable;
import io.reactivex.functions.Predicate;

import java.net.SocketAddress;

import static io.reactivesocket.client.KeepAliveProvider.never;
import static io.reactivesocket.client.SetupProvider.keepAlive;

/**
 * Created by Maksym Ostroverkhov on 28.02.17.
 */
class Client {
    private final ClientConfig clientConfig;
    private final ClientContext clientContext;

    public Client(ClientConfig clientConfig,
                  ClientContext clientContext) {
        this.clientConfig = clientConfig;
        this.clientContext = clientContext;
    }

    public <T> Flowable<ReadResponse<T>> dataWindow(ReadRequest readRequest, Class<T> clazz) {
        return dataWindowFlow(readRequest, clazz);
    }

    public <T> Flowable<WriteResponse> write(WriteRequest<T> writeRequest) {
        return writeFlow(writeRequest);
    }

    public Flowable<DeleteResponse> delete(DeleteRequest deleteRequest) {
        return deleteFlow(deleteRequest);
    }

    private Flowable<DeleteResponse> deleteFlow(DeleteRequest deleteRequest) {
        Flowable<DeleteResponse> deleteResponseFlow = rsocket()
                .compose(upstream -> request(upstream, deleteRequest))
                .map(payload -> toDeleteResponse(
                        clientContext.gson(),
                        payload));
        return deleteResponseFlow;
    }

    private <T> Flowable<WriteResponse> writeFlow(WriteRequest<T> writeRequest) {
        Flowable<WriteResponse> writeResponseFlow = rsocket()
                .compose(upstream -> request(upstream, writeRequest))
                .map(payload -> toWriteResponse(
                        clientContext.gson(),
                        payload));
        return writeResponseFlow;
    }

    private <T> Flowable<ReadResponse<T>> dataWindowFlow(
            ReadRequest readRequest,
            Class<T> clazz) {

        readRequest.setOp(Op.DATA_WINDOW.code());

        Flowable<ReadResponse<T>> readResponseFlow = rsocket()
                .compose(upstream -> request(upstream, readRequest))
                .map(payload -> toReadResponse(
                        clientContext.gson(),
                        payload,
                        clazz));
        return readResponseFlow;
    }

    private <T> Flowable<Payload> request(Flowable<ReactiveSocket> socketFlow,
                                          Object payload) {
        return socketFlow
                .flatMap(socket -> socket
                        .requestStream(toPayload(
                                clientContext.gson(),
                                payload)))
                .filter(requestStreamDataFrames());
    }

    private Flowable<ReactiveSocket> rsocket() {
        SocketAddress address = clientConfig.getSocketAddress();
        return Flowable.fromPublisher(
                ReactiveSocketClient.create(TcpTransportClient.create(address),
                        keepAlive(never()).disableLease())
                        .connect());
    }

    /*workaround for https://github.com/ReactiveSocket/reactivesocket-java/issues/226*/
    private static Predicate<Payload> requestStreamDataFrames() {
        return payload -> (payload instanceof Frame)
                && (((Frame) payload).getType() != FrameType.NEXT_COMPLETE);
    }

    private static <T> ReadResponse<T> toReadResponse(Gson gson,
                                                      Payload payload,
                                                      Class<T> itemType) {
        String dataStr = ByteBufferUtil.toUtf8String(payload.getData());
        return GsonUtil.mapReadResponse(gson, dataStr, itemType);
    }

    private static WriteResponse toWriteResponse(Gson gson,
                                                 Payload payload) {
        String dataStr = ByteBufferUtil.toUtf8String(payload.getData());
        return GsonUtil.mapWriteResponse(gson, dataStr);
    }

    private static DeleteResponse toDeleteResponse(Gson gson,
                                                   Payload payload) {
        String dataStr = ByteBufferUtil.toUtf8String(payload.getData());
        return gson.fromJson(dataStr, DeleteResponse.class);
    }

    private Payload toPayload(Gson gson, Object request) {
        return new PayloadImpl(gson.toJson(request));
    }

}