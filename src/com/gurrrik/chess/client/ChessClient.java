package com.gurrrik.chess.client;

import com.gurrrik.chess.protos.Messages;

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
    private void handleInput(Channel ch) throws Exception {
        Pattern startGamePattern = Pattern.compile("^start\\s+(\\d+)$", Pattern.CASE_INSENSITIVE);
        Pattern movePattern = Pattern.compile("^move\\s+(\\w+)", Pattern.CASE_INSENSITIVE);

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        for (String line = in.readLine(); line != null; line = in.readLine()) {
            Matcher startGameMatch = startGamePattern.matcher(line);
            if (startGameMatch.matches()) {
                long gameId;
                try {
                    gameId = Integer.parseInt(startGameMatch.group(1));
                } catch (NumberFormatException e) {
                    System.err.println(e.toString());
                    continue;
                }

                Messages.MClientMessage.MStartGame.Builder msgStartGameBuilder
                        = Messages.MClientMessage.MStartGame.newBuilder();
                msgStartGameBuilder.setGameId(gameId);

                Messages.MClientMessage.Builder msgBuilder
                        = Messages.MClientMessage.newBuilder();
                msgBuilder.setType(Messages.MClientMessage.EType.START_GAME);
                msgBuilder.setStartGame(msgStartGameBuilder.build());

                Messages.MClientMessage msg = msgBuilder.build();
                ch.writeAndFlush(msg).sync();

                continue;
            }

            Matcher moveMatch = movePattern.matcher(line);
            if (moveMatch.matches()) {
                continue;
            }

            System.err.println("Unrecognized command");
        }
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
                                    .addLast(new ProtobufDecoder(Messages.MServerMessage.getDefaultInstance()))
                                    .addLast(new ProtobufEncoder())
                                    .addLast(new ChessClientHandler());
                        }
                    });
            Channel ch = b.connect(server, port).sync().channel();
            handleInput(ch);
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
