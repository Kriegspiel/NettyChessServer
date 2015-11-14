package com.gurrrik.chess.client;

import com.gurrrik.chess.protos.Messages.MServerMessage;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.ArrayList;
import java.util.List;

public class ChessClientHandler extends SimpleChannelInboundHandler<MServerMessage> {
    private static final String CHESSBOARD_FORMAT_STRING =
            "   a b c d e f g h  \n" +
            " + - - - - - - - - +\n" +
            "8| %s |8\n" +
            "7| %s |7\n" +
            "6| %s |6\n" +
            "5| %s |5\n" +
            "4| %s |4\n" +
            "3| %s |3\n" +
            "2| %s |2\n" +
            "1| %s |1\n" +
            " + - - - - - - - -+\n" +
            "   a b c d e f g h";

    protected void handleMessage(ChannelHandlerContext ctx,
                                 MServerMessage.MGameStarted msg) {
        System.err.println(msg.getGameId());
        System.err.println(msg.getSide().toString());
        System.err.println(msg.getGameType().toString());
    }

    protected void handleMessage(ChannelHandlerContext ctx,
                                 MServerMessage.MMoveResp msg) {
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

    private static String makeChessboardText(String chessboardState) {
        List<String> rows = new ArrayList<String>(8);
        StringBuilder sb = new StringBuilder();

        for (String row: chessboardState.split("/")) {
            sb.setLength(0);
            for (char c: row.toCharArray()) {
                if (Character.isDigit(c)) {
                    for (int i = c - '0'; i > 0; --i) {
                        sb.append(". ");
                    }
                } else {
                    sb.append(c);
                    sb.append(' ');
                }
            }
            assert sb.length() == 16;
            sb.setLength(15);
            rows.add(sb.toString());
        }

        return String.format(CHESSBOARD_FORMAT_STRING,
                rows.get(0), rows.get(1), rows.get(2), rows.get(3),
                rows.get(4), rows.get(5), rows.get(6), rows.get(7));
    }

    protected void handleMessage(ChannelHandlerContext ctx,
                                 MServerMessage.MStateUpdate msg) {
        System.err.println(msg.getNewState());
        String gameState = msg.getNewState();
        String boardState = gameState.split("\\s+")[0];
        System.out.println(makeChessboardText(boardState));
    }

    protected void handleMessage(ChannelHandlerContext ctx,
                                 MServerMessage.MGameOver msg) {
        System.err.println(msg.getResult().toString());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx,
                                MServerMessage msg) throws Exception {
        System.err.println(msg.getType().toString());
        switch (msg.getType()) {
            case GAME_STARTED:
                if (!msg.hasGameStarted()) {
                    System.err.println("Malformed message: " + msg.toString());
                    return;
                }
                handleMessage(ctx, msg.getGameStarted());
                break;
            case MOVE_RESP:
                if (!msg.hasMoveResp()) {
                    System.err.println("Malformed message: " + msg.toString());
                    return;
                }
                handleMessage(ctx, msg.getMoveResp());
                break;
            case STATE_UPDATE:
                if (!msg.hasStateUpdate()) {
                    System.err.println("Malformed message: " + msg.toString());
                    return;
                }
                handleMessage(ctx, msg.getStateUpdate());
                break;
            case GAME_OVER:
                if (!msg.hasGameOver()) {
                    System.err.println("Malformed message: " + msg.toString());
                    return;
                }
                handleMessage(ctx, msg.getGameOver());
                break;
            default:
                break;
        }
    }
}
