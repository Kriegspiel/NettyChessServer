package com.gurrrik.chess.server;

import com.gurrrik.chess.protos.Messages.EGameType;
import com.gurrrik.chess.protos.Messages.MClientMessage;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ChannelHandler.Sharable
public class ChessServerHandler extends SimpleChannelInboundHandler<MClientMessage> {
    private Map<Long, ChessGameRoom> games = new ConcurrentHashMap<>();
    private Map<SocketAddress, Long> playersInGame = new ConcurrentHashMap<>();

    protected void handleMessage(ChannelHandlerContext ctx,
                                 MClientMessage.MStartGame msg) throws Exception {
        SocketAddress playerAddress = ctx.channel().remoteAddress();
        if (playersInGame.containsKey(playerAddress)) {
            System.err.println("Player already in game: " + playerAddress.toString());
            return;
        }

        long gameId = msg.getGameId();
        EGameType gameType = msg.getGameType();
        if (games.containsKey(gameId)) {
            ChessGameRoom gameRoom = games.get(gameId);
            if (!gameRoom.hasRoom()) {
                System.err.println("The room is full: " + gameId);
                return;
            } else {
                gameRoom.setNewPlayer(playerAddress, ctx.channel());
                System.err.println("Added player " + playerAddress.toString() + " to game room " + gameId);
            }
        } else {
            ChessGameRoom gameRoom = new ChessGameRoom(gameId, gameType);
            games.put(gameId, gameRoom);
            System.err.println("Created new game room for " + gameId);
            gameRoom.setNewPlayer(playerAddress, ctx.channel());
            System.err.println("Added player " + playerAddress.toString() + " to game room " + gameId);
        }

        playersInGame.put(playerAddress, gameId);
    }

    protected void handleMessage(ChannelHandlerContext ctx,
                                 MClientMessage.MMove msg) throws Exception {
        SocketAddress playerAddress = ctx.channel().remoteAddress();
        if (!playersInGame.containsKey(playerAddress)) {
            System.err.println("Player is not in game: " + playerAddress.toString());
            return;
        }

        System.err.println("Attempted move by player " + playerAddress.toString());

        long gameId = playersInGame.get(playerAddress);
        ChessGameRoom gameRoom = games.get(gameId);
        if (gameRoom.hasRoom()) {
            System.err.println("Game has not yet started");
            return;
        }
        gameRoom.makeMove(playerAddress, msg.getSqFrom(), msg.getSqTo(), msg.getPromoPiece());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx,
                                MClientMessage msg) throws Exception {
        System.err.println(msg.getType().toString());
        switch (msg.getType()) {
            case START_GAME:
                if (!msg.hasStartGame()) {
                    System.err.println("Malformed message: " + msg.toString());
                    return;
                }
                handleMessage(ctx, msg.getStartGame());
                break;
            case MOVE:
                if (!msg.hasMove()) {
                    System.err.println("Malformed message: " + msg.toString());
                    return;
                }
                handleMessage(ctx, msg.getMove());
                break;
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        SocketAddress playerAddress = ctx.channel().remoteAddress();

        if (playersInGame.containsKey(playerAddress)) {
            long gameId = playersInGame.get(playerAddress);
            ChessGameRoom gameRoom = games.get(gameId);

            SocketAddress player1 = gameRoom.getPlayerWhite();
            SocketAddress player2 = gameRoom.getPlayerBlack();
            gameRoom.playerDisconnected(playerAddress);

            games.remove(gameId);
            if (player1 != null)
                playersInGame.remove(player1);
            if (player2 != null)
                playersInGame.remove(player2);
        }
    }
}
