package com.gurrrik.chess.server;

import com.gurrrik.chess.protos.Messages.MClientMessage;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.util.concurrent.EventExecutorGroup;

public class ChessServerInitializer extends ChannelInitializer<SocketChannel> {
    static ProtobufVarint32LengthFieldPrepender frameEncoderInstance =
            new ProtobufVarint32LengthFieldPrepender();
    static ProtobufDecoder protobufDecoderInstance =
            new ProtobufDecoder(MClientMessage.getDefaultInstance());
    static ProtobufEncoder protobufEncoderInstance = new ProtobufEncoder();
    static ChessServerHandler chessGameHandlerInstance = new ChessServerHandler();

    private EventExecutorGroup eventExecutorGroup;

    public ChessServerInitializer(EventExecutorGroup eeGroup) {
        eventExecutorGroup = eeGroup;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline()
                .addLast(new ProtobufVarint32FrameDecoder())
                .addLast(frameEncoderInstance)
                .addLast(protobufDecoderInstance)
                .addLast(protobufEncoderInstance)
                .addLast(eventExecutorGroup, chessGameHandlerInstance);
    }
}
