package com.github.mostroverkhov.firebase_rsocket.transport.tcp;

import com.github.mostroverkhov.firebase_rsocket.transport.ClientTransport;
import io.reactivesocket.transport.TransportClient;
import io.reactivesocket.transport.tcp.client.TcpTransportClient;

import java.net.SocketAddress;

/**
 * Created with IntelliJ IDEA.
 * Author: mostroverkhov
 */
public class ClientTransportTcp implements ClientTransport {
    private final SocketAddress socketAddress;

    public ClientTransportTcp(SocketAddress socketAddress) {
        assertAddress(socketAddress);
        this.socketAddress = socketAddress;
    }

    @Override
    public TransportClient client() {
        return TcpTransportClient.create(socketAddress);
    }

    private void assertAddress(SocketAddress socketAddress) {
        if (socketAddress == null) {
            throw new IllegalArgumentException("SocketAddress should not be null");
        }
    }
}
