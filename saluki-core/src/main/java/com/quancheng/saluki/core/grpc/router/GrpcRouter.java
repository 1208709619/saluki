/*
 * Copyright (c) 2016, Quancheng-ec.com All right reserved. This software is the
 * confidential and proprietary information of Quancheng-ec.com ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Quancheng-ec.com.
 */
package com.quancheng.saluki.core.grpc.router;

import java.net.SocketAddress;

import com.quancheng.saluki.core.common.GrpcURL;

/**
 * @author shimingliu 2016年12月29日 下午7:52:14
 * @version GrpcRouter.java, v 0.0.1 2016年12月29日 下午7:52:14 shimingliu
 */
public abstract class GrpcRouter {

    private final String  routerMessage;

    private final GrpcURL url;

    public GrpcRouter(GrpcURL url, String routerMessage){
        this.routerMessage = routerMessage;
        this.url = url;
        parseRouter();
    }

    public String getRouterMessage() {
        return routerMessage;
    }

    public GrpcURL getUrl() {
        return url;
    }

    protected abstract void parseRouter();

    public abstract boolean match(SocketAddress server);
}