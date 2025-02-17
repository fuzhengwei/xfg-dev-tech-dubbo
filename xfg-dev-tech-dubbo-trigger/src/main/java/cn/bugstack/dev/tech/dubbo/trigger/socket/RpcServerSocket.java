package cn.bugstack.dev.tech.dubbo.trigger.socket;

import com.alibaba.fastjson.JSON;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class RpcServerSocket implements Runnable {

    private ApplicationContext applicationContext;

    public RpcServerSocket(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        new Thread(this).start();
    }

    @Override
    public void run() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel channel) {
                            channel.pipeline().addLast(new ObjectEncoder());
                            channel.pipeline().addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
                            channel.pipeline().addLast(new SimpleChannelInboundHandler<Map<String, Object>>() {

                                @Override
                                protected void channelRead0(ChannelHandlerContext channelHandlerContext, Map<String, Object> request) throws Exception {
                                    // 解析参数
                                    Class<?> clazz = (Class<?>) request.get("clazz");
                                    String methodName = (String) request.get("methodName");
                                    Class<?>[] paramTypes = (Class<?>[]) request.get("paramTypes");
                                    Object[] args = (Object[]) request.get("args");

                                    // 反射调用
                                    Method method = clazz.getMethod(methodName, paramTypes);
                                    Object invoke = method.invoke(applicationContext.getBean(clazz), args);

                                    // 封装结果
                                    Map<String, Object> response = new HashMap<>();
                                    response.put("data", invoke);

                                    log.info("RPC 请求调用 clazz:{} methodName:{}, response:{}", clazz.getName(), methodName, JSON.toJSON(response));
                                    // 回写数据
                                    channelHandlerContext.channel().writeAndFlush(response);
                                }
                            });
                        }
                    });

            ChannelFuture f = b.bind(22881).sync();
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}
