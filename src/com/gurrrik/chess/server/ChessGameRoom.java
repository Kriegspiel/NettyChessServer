package com.gurrrik.chess.server;

import chesspresso.Chess;
import chesspresso.game.Game;
import chesspresso.move.IllegalMoveException;
import chesspresso.position.Position;
import com.gurrrik.chess.protos.Messages.EGameType;
import com.gurrrik.chess.protos.Messages.MServerMessage;
import io.netty.channel.Channel;

import java.net.SocketAddress;
import java.util.Arrays;

class KriegspielBoardFilter {
    static String filter(String position, int player) {
        String[] stringPieces = position.split("\\s+");

        StringBuilder boardBuilder = new StringBuilder();
        StringBuilder opponentPiecesBuilder = new StringBuilder();
        for (char c: stringPieces[0].toCharArray()) {
            switch (player) {
                case Chess.WHITE:
                    if (Character.isLowerCase(c)) {
                        boardBuilder.append('1');
                        opponentPiecesBuilder.append(c);
                    } else {
                        boardBuilder.append(c);
                    }
                    break;
                case Chess.BLACK:
                    if (Character.isUpperCase(c)) {
                        boardBuilder.append('1');
                        opponentPiecesBuilder.append(c);
                    } else {
                        boardBuilder.append(c);
                    }
                    break;
                default:
                    break;
            }
        }
        String filtered = boardBuilder.toString();

        boardBuilder = new StringBuilder();
        int num = 0;
        boolean onNum = false;
        for (char c: filtered.toCharArray()) {
            if (Character.isDigit(c)) {
                onNum = true;
                num += c - '0';
            } else {
                if (onNum) {
                    onNum = false;
                    boardBuilder.append(num);
                    num = 0;
                }
                boardBuilder.append(c);
            }
        }
        if (onNum)
            boardBuilder.append(num);

        char[] pieces = opponentPiecesBuilder.toString().toCharArray();
        Arrays.sort(pieces);
        String sortedOpponentPieces = new String(pieces);

        stringPieces[0] = boardBuilder.toString() + " " + sortedOpponentPieces;
        return String.join(" ", stringPieces);
    }
}

public class ChessGameRoom {
    private long gameId;
    private EGameType gameType;
    private Game chessGame;
    private boolean finished;

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

    public ChessGameRoom(long id, EGameType type) {
        gameId = id;
        gameType = type;
        chessGame = new Game();
        finished = false;
    }

    private void sendGameStarted() {
        MServerMessage.MGameStarted.Builder msgGameStartedBuilder
                = MServerMessage.MGameStarted.newBuilder();
        msgGameStartedBuilder.setGameId(gameId);
        msgGameStartedBuilder.setSide(MServerMessage.MGameStarted.ESide.WHITE);
        msgGameStartedBuilder.setGameType(gameType);

        MServerMessage.Builder msgBuilder
                = MServerMessage.newBuilder();
        msgBuilder.setType(MServerMessage.EType.GAME_STARTED);
        msgBuilder.setGameStarted(msgGameStartedBuilder.build());

        MServerMessage msgWhite = msgBuilder.build();

        msgGameStartedBuilder.setSide(MServerMessage.MGameStarted.ESide.BLACK);
        msgBuilder.setGameStarted(msgGameStartedBuilder.build());

        MServerMessage msgBlack = msgBuilder.build();

        playerWhiteChannel.writeAndFlush(msgWhite);
        playerBlackChannel.writeAndFlush(msgBlack);
    }

    private void sendGameState() {
        switch (gameType) {
            case CHESS:
                sendGameStateChess();
                break;
            case KRIEGSPIEL:
                sendGameStateKriegspiel();
                break;
            default:
                break;
        }
    }

    private void sendGameStateChess() {
        MServerMessage.MStateUpdate.Builder msgStateUpdateBuilder
                = MServerMessage.MStateUpdate.newBuilder();
        msgStateUpdateBuilder.setNewState(chessGame.getPosition().toString());

        MServerMessage.Builder msgBuilder
                = MServerMessage.newBuilder();
        msgBuilder.setType(MServerMessage.EType.STATE_UPDATE);
        msgBuilder.setStateUpdate(msgStateUpdateBuilder.build());

        MServerMessage msg = msgBuilder.build();

        playerWhiteChannel.writeAndFlush(msg);
        playerBlackChannel.writeAndFlush(msg);
    }

    private void sendGameStateKriegspiel() {
        MServerMessage.MStateUpdate.Builder msgStateUpdateBuilder
                = MServerMessage.MStateUpdate.newBuilder();

        MServerMessage.Builder msgBuilder
                = MServerMessage.newBuilder();
        msgBuilder.setType(MServerMessage.EType.STATE_UPDATE);

        msgStateUpdateBuilder.setNewState(
                KriegspielBoardFilter.filter(chessGame.getPosition().toString(), Chess.WHITE));
        msgBuilder.setStateUpdate(msgStateUpdateBuilder.build());
        playerWhiteChannel.writeAndFlush(msgBuilder.build());

        msgStateUpdateBuilder.setNewState(
                KriegspielBoardFilter.filter(chessGame.getPosition().toString(), Chess.BLACK));
        msgBuilder.setStateUpdate(msgStateUpdateBuilder.build());
        playerBlackChannel.writeAndFlush(msgBuilder.build());
    }

    private void sendMoveResponse(int player, boolean response) {
        MServerMessage.MMoveResp.Builder msgMoveRespBuilder
                = MServerMessage.MMoveResp.newBuilder();
        msgMoveRespBuilder.setResponse(response
                ? MServerMessage.MMoveResp.EResponse.SUCCESS
                : MServerMessage.MMoveResp.EResponse.FAILURE);

        MServerMessage.Builder msgBuilder
                = MServerMessage.newBuilder();
        msgBuilder.setType(MServerMessage.EType.MOVE_RESP);
        msgBuilder.setMoveResp(msgMoveRespBuilder.build());

        MServerMessage msg = msgBuilder.build();

        switch (player) {
            case Chess.WHITE:
                playerWhiteChannel.writeAndFlush(msg);
                break;
            case Chess.BLACK:
                playerBlackChannel.writeAndFlush(msg);
                break;
            default:
                break;
        }
    }

    private void sendGameOver(int result) {
        MServerMessage.MGameOver.Builder msgGameOverBuilder
                = MServerMessage.MGameOver.newBuilder();
        switch (result) {
            case Chess.RES_WHITE_WINS:
                msgGameOverBuilder.setResult(MServerMessage.MGameOver.EResult.WHITE);
                break;
            case Chess.RES_BLACK_WINS:
                msgGameOverBuilder.setResult(MServerMessage.MGameOver.EResult.BLACK);
                break;
            case Chess.RES_DRAW:
                msgGameOverBuilder.setResult(MServerMessage.MGameOver.EResult.DRAW);
                break;
            default:
                return;
        }

        MServerMessage.Builder msgBuilder
                = MServerMessage.newBuilder();
        msgBuilder.setType(MServerMessage.EType.GAME_OVER);
        msgBuilder.setGameOver(msgGameOverBuilder.build());

        MServerMessage msg = msgBuilder.build();

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

    public synchronized void makeMove(SocketAddress playerAddress, int sqrFrom, int sqrTo, int promoPiece) {
        if (finished)
            return;

        if (promoPiece < 0)
            promoPiece = 0;

        Position pos = chessGame.getPosition();
        if (playerAddress.equals(playerWhite)) {
            if (pos.getToPlay() == Chess.WHITE) {
                try {
                    pos.doMove(pos.getMove(sqrFrom, sqrTo, promoPiece));
                    sendMoveResponse(Chess.WHITE, true);
                    sendGameState();
                } catch (IllegalMoveException e) {
                    sendMoveResponse(Chess.WHITE, false);
                }
            } else {
                sendMoveResponse(Chess.WHITE, false);
            }
        } else if (playerAddress.equals(playerBlack)) {
            if (pos.getToPlay() == Chess.BLACK) {
                try {
                    pos.doMove(pos.getMove(sqrFrom, sqrTo, promoPiece));
                    sendMoveResponse(Chess.BLACK, true);
                    sendGameState();
                } catch (IllegalMoveException e) {
                    sendMoveResponse(Chess.BLACK, false);
                }
            } else {
                sendMoveResponse(Chess.BLACK, false);
            }
        } else {
            throw new RuntimeException("No such player in game: " + playerAddress.toString());
        }

        if (pos.isStaleMate()) {
            finished = true;
            sendGameOver(Chess.RES_DRAW);
        } else if (pos.isMate()) {
            finished = true;
            switch (pos.getToPlay()) {
                case Chess.WHITE:
                    sendGameOver(Chess.RES_BLACK_WINS);
                    break;
                case Chess.BLACK:
                    sendGameOver(Chess.RES_WHITE_WINS);
                    break;
            }
        }
    }

    public synchronized void playerDisconnected(SocketAddress playerAddress) {
        finished = true;
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
