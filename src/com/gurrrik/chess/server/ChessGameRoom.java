package com.gurrrik.chess.server;

import chesspresso.game.Game;
import com.gurrrik.chess.protos.Messages;
import io.netty.channel.Channel;

import java.net.SocketAddress;

public class ChessGameRoom {
    private long gameId;
    private Game chessGame;
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

    public void setNewPlayer(SocketAddress addr, Channel ch) throws Exception {
        if (playerWhite == null) {
            playerWhite = addr;
            playerWhiteChannel = ch;
        } else if (playerBlack == null) {
            playerBlack = addr;
            playerBlackChannel = ch;
            sendGameStarted();
        } else {
            throw new RuntimeException("Game room is already full!");
        }
    }

    public boolean hasRoom() {
        return playerBlack == null;
    }
}
