package com.gurrrik.chess.server;

import chesspresso.Chess;
import chesspresso.game.Game;
import com.gurrrik.chess.protos.Messages;
import io.netty.channel.Channel;

import java.net.SocketAddress;

public class ChessGameRoom {
    private long gameId;
    private Game chessGame;

    public SocketAddress getPlayerWhite() {
        return playerWhite;
    }

    public SocketAddress getPlayerBlack() {
        return playerBlack;
    }

    private SocketAddress playerWhite;
    private Channel playerWhiteChannel;
    private SocketAddress playerBlack;
    private Channel playerBlackChannel;

    public ChessGameRoom(long id) {
        gameId = id;
        chessGame = new Game();
    }

    private void sendGameStarted() {
        Messages.MServerMessage.MGameStarted.Builder msgGameStartedBuilder
                = Messages.MServerMessage.MGameStarted.newBuilder();
        msgGameStartedBuilder.setGameId(gameId);

        Messages.MServerMessage.Builder msgBuilder
                = Messages.MServerMessage.newBuilder();
        msgBuilder.setType(Messages.MServerMessage.EType.GAME_STARTED);
        msgBuilder.setGameStarted(msgGameStartedBuilder.build());

        Messages.MServerMessage msg = msgBuilder.build();

        playerWhiteChannel.writeAndFlush(msg);
        playerBlackChannel.writeAndFlush(msg);
    }

    private void sendGameState() {
        Messages.MServerMessage.MStateUpdate.Builder msgStateUpdateBuilder
                = Messages.MServerMessage.MStateUpdate.newBuilder();
        msgStateUpdateBuilder.setNewState(chessGame.getPosition().toString());

        Messages.MServerMessage.Builder msgBuilder
                = Messages.MServerMessage.newBuilder();
        msgBuilder.setType(Messages.MServerMessage.EType.STATE_UPDATE);
        msgBuilder.setStateUpdate(msgStateUpdateBuilder.build());

        Messages.MServerMessage msg = msgBuilder.build();

        playerWhiteChannel.writeAndFlush(msg);
        playerBlackChannel.writeAndFlush(msg);
    }

    private void sendGameOver(int result) {
        Messages.MServerMessage.MGameOver.Builder msgGameOverBuilder
                = Messages.MServerMessage.MGameOver.newBuilder();
        switch (result) {
            case Chess.RES_WHITE_WINS:
                msgGameOverBuilder.setResult(Messages.MServerMessage.MGameOver.EResult.WHITE);
                break;
            case Chess.RES_BLACK_WINS:
                msgGameOverBuilder.setResult(Messages.MServerMessage.MGameOver.EResult.BLACK);
                break;
            case Chess.RES_DRAW:
                msgGameOverBuilder.setResult(Messages.MServerMessage.MGameOver.EResult.DRAW);
                break;
        }

        Messages.MServerMessage.Builder msgBuilder
                = Messages.MServerMessage.newBuilder();
        msgBuilder.setType(Messages.MServerMessage.EType.GAME_OVER);
        msgBuilder.setGameOver(msgGameOverBuilder.build());

        Messages.MServerMessage msg = msgBuilder.build();

        if (playerWhiteChannel != null)
            playerWhiteChannel.writeAndFlush(msg);
        if (playerBlackChannel != null)
            playerBlackChannel.writeAndFlush(msg);
    }

    public synchronized void setNewPlayer(SocketAddress addr, Channel ch) throws Exception {
        if (playerWhite == null) {
            playerWhite = addr;
            playerWhiteChannel = ch;
        } else if (playerBlack == null) {
            playerBlack = addr;
            playerBlackChannel = ch;
            sendGameStarted();
            sendGameState();
        } else {
            throw new RuntimeException("Game room is already full!");
        }
    }

    public synchronized boolean hasRoom() {
        return playerBlack == null;
    }

    public synchronized void playerDisconnected(SocketAddress playerAddress) {
        if (playerAddress.equals(playerWhite)) {
            playerWhite = null;
            playerWhiteChannel = null;
            sendGameOver(Chess.RES_BLACK_WINS);
        } else if (playerAddress.equals(playerBlack)) {
            playerBlack = null;
            playerBlackChannel = null;
            sendGameOver(Chess.RES_WHITE_WINS);
        } else {
            throw new RuntimeException("No such player in game: " + playerAddress.toString());
        }
    }
}
