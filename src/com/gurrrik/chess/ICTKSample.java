package com.gurrrik.chess;

import ictk.boardgame.IllegalMoveException;
import ictk.boardgame.chess.ChessBoard;
import ictk.boardgame.chess.ChessGame;
import ictk.boardgame.chess.ChessMove;

public class ICTKSample {
    public static void main(String[] args) throws IllegalMoveException {
        ChessGame game = new ChessGame(null, new ChessBoard(ChessBoard.DEFAULT_POSITION));
        ChessBoard board = (ChessBoard)game.getBoard();
        board.playMove(new ChessMove(
                board,
                board.getSquare('e', '2'),
                board.getSquare('e', '4')));
        board.playMove(new ChessMove(
                board,
                board.getSquare('e', '7'),
                board.getSquare('e', '5')));
        board.playMove(new ChessMove(
                board,
                board.getSquare('d', '2'),
                board.getSquare('d', '4')));
    }
}
