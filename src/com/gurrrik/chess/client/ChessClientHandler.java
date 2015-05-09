package com.gurrrik.chess.client;

import com.gurrrik.chess.protos.Messages;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ChessClientHandler extends SimpleChannelInboundHandler<Messages.MServerMessage> {
    protected void handleGameStartedMessage(ChannelHandlerContext ctx,
                                            Messages.MServerMessage.MGameStarted msg) {
        System.err.println(msg.getGameId());
        System.err.println(msg.getSide().toString());
    }

    protected void handleMoveResponseMessage(ChannelHandlerContext ctx,
                                             Messages.MServerMessage.MMoveResp msg) {
        System.err.println(msg.getResponse().toString());
    }

    private static String expandFENLine(String line) {
        StringBuilder sb = new StringBuilder();
        for (char c: line.toCharArray()) {
            if (Character.isDigit(c)) {
                for (int i = c - '0'; i > 0; --i)
                    sb.append('.');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    protected void handleStateUpdateMessage(ChannelHandlerContext ctx,
                                            Messages.MServerMessage.MStateUpdate msg) {
        System.err.println(msg.getNewState());
        String gameState = msg.getNewState();
        String boardState = gameState.split("\\s+")[0];
        String[] lines = boardState.split("/");

        StringBuilder sb = new StringBuilder();
        for (String line: lines) {
            sb.append(expandFENLine(line));
            sb.append("\n");
        }
        System.out.println(sb.toString());
    }

    protected void handleGameOverMessage(ChannelHandlerContext ctx,
                                         Messages.MServerMessage.MGameOver msg) {
        System.err.println(msg.getResult().toString());
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
            case GAME_OVER:
                if (!msg.hasGameOver()) {
                    System.err.println("Malformed message: " + msg.toString());
                    return;
                }
                handleGameOverMessage(ctx, msg.getGameOver());
                break;
            default:
                break;
        }
    }
}
