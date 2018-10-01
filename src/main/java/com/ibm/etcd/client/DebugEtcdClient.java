package com.ibm.etcd.client;

import com.google.protobuf.ByteString;
import io.grpc.netty.NettyChannelBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class DebugEtcdClient extends EtcdClient {

    private static final long IDLE_TIMEOUT = 5_000L;

    DebugEtcdClient(NettyChannelBuilder chanBuilder, long defaultTimeoutMs, ByteString name, ByteString password, boolean initialAuth, int threads, Executor userExecutor, boolean sendViaEventLoop, int sessTimeoutSecs) {
        super(chanBuilder, defaultTimeoutMs, name, password, initialAuth, threads, userExecutor, sendViaEventLoop, sessTimeoutSecs);
    }
    
    public static Builder forEndpoints(List<String> endpoints) {
        NettyChannelBuilder ncb = NettyChannelBuilder
                .forTarget(StaticEtcdNameResolverFactory.ETCD)
                .nameResolverFactory(new StaticEtcdNameResolverFactory(endpoints))
                .idleTimeout(IDLE_TIMEOUT, TimeUnit.MILLISECONDS);
        return new Builder(ncb);
    }

    public static Builder forEndpoints(String endpoints) {
        return forEndpoints(Arrays.asList(endpoints.split(",")));
    }
}
