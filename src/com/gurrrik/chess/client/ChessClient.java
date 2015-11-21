package com.gurrrik.chess.client;

import chesspresso.Chess;
import com.gurrrik.chess.protos.Messages.EGameType;
import com.gurrrik.chess.protos.Messages.MClientMessage;
import com.gurrrik.chess.protos.Messages.MServerMessage;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class CLArgs {
    @Parameter
    public List<String> parameters = new ArrayList<>();

    @Parameter(names={"-s", "-server"}, description="Server address")
    public String server = "127.0.0.1";

    @Parameter(names={"-p", "-port"}, description="Server port")
    public int port = 8000;
}

public class ChessClient {
    private Channel channel;

    private final Pattern startGamePattern = Pattern.compile("^start\\s+(\\d+)(?:\\s+(\\w+))?$", Pattern.CASE_INSENSITIVE);
    private final Pattern movePattern = Pattern.compile("^move\\s+((?:\\w\\d){2})(?:\\s+(\\w))?$", Pattern.CASE_INSENSITIVE);

    private final Map<String, EGameType> gameTypeNames = new HashMap<>();
    private final EGameType defaultGameType = EGameType.CHESS;

    public ChessClient() {
        gameTypeNames.put("chess", EGameType.CHESS);
        gameTypeNames.put("kriegspiel", EGameType.KRIEGSPIEL);
    }

    private void handleInput() throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        for (String line = in.readLine(); line != null; line = in.readLine())
            handleStringCommand(line);
    }

    private void handleStringCommand(String command) throws Exception {
        Matcher startGameMatch = startGamePattern.matcher(command);
        if (startGameMatch.matches()) {
            long gameId;
            String gameType;
            try {
                gameId = Integer.parseInt(startGameMatch.group(1));
                gameType = startGameMatch.group(2);
            } catch (NumberFormatException e) {
                System.err.println(e.toString());
                return;
            }

            System.err.println("Starting game " + gameId);

            handleStartInput(gameId, gameType);

            return;
        }

        Matcher moveMatch = movePattern.matcher(command);
        if (moveMatch.matches()) {
            int sqFrom, sqTo, promoPiece;
            sqFrom = Chess.strToSqi(moveMatch.group(1).charAt(0), moveMatch.group(1).charAt(1));
            sqTo = Chess.strToSqi(moveMatch.group(1).charAt(2), moveMatch.group(1).charAt(3));
            if (sqFrom == -1 || sqTo == -1) {
                System.err.println("Wrong move");
                return;
            }

            try {
                if (moveMatch.group(2) == null)
                    promoPiece = Chess.NO_PIECE;
                else
                    promoPiece = Chess.charToPiece(moveMatch.group(2).charAt(0));
            } catch (NumberFormatException e) {
                System.err.println(e.toString());
                return;
            }

            handleMoveInput(sqFrom, sqTo, promoPiece);

            return;
        }

        System.err.println("Unrecognized command");
    }

    private void handleStartInput(long gameId, String gameType) throws Exception {
        MClientMessage.MStartGame.Builder msgStartGameBuilder =
                MClientMessage.MStartGame.newBuilder();
        msgStartGameBuilder.setGameId(gameId);
        if (gameType != null || gameTypeNames.containsKey(gameType))
            msgStartGameBuilder.setGameType(gameTypeNames.get(gameType));
        else
            msgStartGameBuilder.setGameType(defaultGameType);

        MClientMessage.Builder msgBuilder = MClientMessage.newBuilder();
        msgBuilder.setType(MClientMessage.EType.START_GAME);
        msgBuilder.setStartGame(msgStartGameBuilder.build());

        MClientMessage msg = msgBuilder.build();
        channel.writeAndFlush(msg).sync();
    }

    private void handleMoveInput(int sqrFrom, int sqrTo, int promoPiece) throws Exception {
        System.err.println("Move " + sqrFrom + " " + sqrTo + " " + promoPiece);

        MClientMessage.MMove.Builder msgMoveBuilder = MClientMessage.MMove.newBuilder();
        msgMoveBuilder.setSqFrom(sqrFrom);
        msgMoveBuilder.setSqTo(sqrTo);
        msgMoveBuilder.setPromoPiece(promoPiece);

        MClientMessage.Builder msgBuilder = MClientMessage.newBuilder();
        msgBuilder.setType(MClientMessage.EType.MOVE);
        msgBuilder.setMove(msgMoveBuilder.build());

        MClientMessage msg = msgBuilder.build();
        channel.writeAndFlush(msg).sync();
    }

    public void startClient(String server, int port) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new ProtobufVarint32FrameDecoder())
                                    .addLast(new ProtobufVarint32LengthFieldPrepender())
                                    .addLast(new ProtobufDecoder(MServerMessage.getDefaultInstance()))
                                    .addLast(new ProtobufEncoder())
                                    .addLast(new ChessClientHandler());
                        }
                    });
            channel = b.connect(server, port).sync().channel();
            handleInput();
        } finally {
            group.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        CLArgs clArgs = new CLArgs();
        new JCommander(clArgs, args);

        new ChessClient().startClient(clArgs.server, clArgs.port);
    }
}
