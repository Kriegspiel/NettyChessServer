package com.gurrrik.chess.client;

import com.gurrrik.chess.protos.Messages;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ChessClientHandler extends SimpleChannelInboundHandler<Messages.MServerMessage> {
    protected void handleGameStartedMessage(ChannelHandlerContext ctx,
                                            Messages.MServerMessage.MGameStarted msg) {
        System.err.println(msg.getGameId());
    }

    protected void handleMoveResponseMessage(ChannelHandlerContext ctx,
                                            Messages.MServerMessage.MMoveResp msg) {

    }

    protected void handleStateUpdateMessage(ChannelHandlerContext ctx,
                                            Messages.MServerMessage.MStateUpdate msg) {

    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx,
                                Messages.MServerMessage msg) throws Exception {
        System.err.println(msg.getType().toString());
        switch (msg.getType()) {
            case GAME_STARTED:
                if (!msg.hasGameStarted()) {
                    System.err.println("Malformed message: " + msg.toString());
                    return;
                }
                handleGameStartedMessage(ctx, msg.getGameStarted());
                break;
            case MOVE_RESP:
                if (!msg.hasMoveResp()) {
                    System.err.println("Malformed message: " + msg.toString());
                    return;
                }
                handleMoveResponseMessage(ctx, msg.getMoveResp());
                break;
            case STATE_UPDATE:
                if (!msg.hasStateUpdate()) {
                    System.err.println("Malformed message: " + msg.toString());
                    return;
                }
                handleStateUpdateMessage(ctx, msg.getStateUpdate());
                break;
        }
    }
}
