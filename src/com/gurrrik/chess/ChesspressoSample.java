package com.gurrrik.chess;

import chesspresso.Chess;
import chesspresso.game.Game;
import chesspresso.move.IllegalMoveException;
import chesspresso.position.Position;

public class ChesspressoSample {
    public static void main(String[] args) {
        Game chessGame = new Game();
        Position p = chessGame.getPosition();
        System.out.println(Chess.A1);
        System.out.println(Chess.A8);
        System.out.println(Chess.H1);
        System.out.println(Chess.H8);
        try {
            p.doMove(p.getMove(Chess.E2, Chess.E4, Chess.NO_PIECE));
            p.doMove(p.getMove(Chess.E7, Chess.E5, Chess.NO_PIECE));
            p.doMove(p.getMove(Chess.G1, Chess.F3, Chess.NO_PIECE));
            p.doMove(p.getMove(Chess.G8, Chess.F6, Chess.NO_PIECE));
            p.doMove(p.getMove(Chess.F1, Chess.B5, Chess.NO_PIECE));
            p.doMove(p.getMove(Chess.A7, Chess.A6, Chess.NO_PIECE));
            p.doMove(p.getMove(Chess.E1, Chess.G1, Chess.NO_PIECE));
        } catch (IllegalMoveException e) {
            System.err.println("Erroneous move: " + e.getMessage());
        }
        System.out.println(p.toString());

        chessGame = new Game();
        p = chessGame.getPosition();
        try {
            p.doMove(p.getMove(Chess.E2, Chess.E4, Chess.NO_PIECE));
            p.doMove(p.getMove(Chess.E7, Chess.E5, Chess.NO_PIECE));
            p.doMove(p.getMove(Chess.E4, Chess.E5, Chess.NO_PIECE));
        } catch (IllegalMoveException e) {
            System.err.println("Erroneous move: " + e.getMessage());
        }
        System.out.println(p.toString());
    }
}
