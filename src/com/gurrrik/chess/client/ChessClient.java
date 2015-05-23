package com.gurrrik.chess.client;

import chesspresso.Chess;
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
import java.util.ArrayList;
import java.util.List;
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
    private JTextArea chessBoardText;
    private JTextField moveTextInput;
    private JLabel sideLabel;

    private Channel channel;

    private final Pattern startGamePattern = Pattern.compile("^start\\s+(\\d+)$", Pattern.CASE_INSENSITIVE);
    private final Pattern movePattern = Pattern.compile("^move\\s+((?:\\w\\d){2})(?:\\s+(\\d+))?$", Pattern.CASE_INSENSITIVE);

    public ChessClient() {
        moveTextInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == 13) {
                    String command = moveTextInput.getText();
                    moveTextInput.setText("");
                    try {
                        handleStringCommand(command);
                    } catch (Exception exc) {
                        System.err.println(exc.getMessage());
                    }
                } else {
                    super.keyPressed(e);
                }
            }
        });
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
            try {
                gameId = Integer.parseInt(startGameMatch.group(1));
            } catch (NumberFormatException e) {
                System.err.println(e.toString());
                return;
            }

            System.err.println("Starting game " + gameId);

            handleStartInput(gameId);

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
                    promoPiece = 0;
                else
                    promoPiece = Integer.parseInt(moveMatch.group(2));
            } catch (NumberFormatException e) {
                System.err.println(e.toString());
                return;
            }

            handleMoveInput(sqFrom, sqTo, promoPiece);

            return;
        }

        System.err.println("Unrecognized command");
    }

    private void handleStartInput(long gameId) throws Exception {
        MClientMessage.MStartGame.Builder msgStartGameBuilder =
                MClientMessage.MStartGame.newBuilder();
        msgStartGameBuilder.setGameId(gameId);

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
