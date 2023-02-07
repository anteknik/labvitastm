package com.github.astm.cli;

import com.github.astm.lib.TypeSideCommunication;
import com.github.astm.lib.exceptions.CreateInstanseException;
import com.github.astm.lib.tcp.TCP;
import com.github.astm.lib.tcp.TCPBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ClientApp {

    private static final int port = 10015;
    private static final String host = "127.0.0.1";

    private static int messageAmount;

    private static Logger log = LoggerFactory.getLogger(ClientApp.class);

    public static void main(String[] args) throws IOException {

        Path scenario = Paths.get("src", "test", "resources", "scenario");
        Sinks.Many<byte[]> flowMessagesForAnalyzer = Sinks
                .many()
                .multicast()
                .onBackpressureBuffer();
        TCP clientTCPanalyzer = TCPBuilder
                .builder(TypeSideCommunication.CLIENT, port)
                .setHost(host)
                .setMessageFlowForNetwork(flowMessagesForAnalyzer.asFlux())
                .build();
        Flux<byte[]> flowMessagesFromAnalyzer = clientTCPanalyzer.getMessageFlowFromNetwork();

        clientTCPanalyzer
                .getStdIn()
                .subscribe(el -> {
                    log.info("STDIN: {}", el);
                });


        clientTCPanalyzer
                .getStdOut()
                .subscribe(el -> {
                    log.info("STDOUT: {}", el);
                });
        clientTCPanalyzer
                .getStdErr()
                .subscribe(el -> {
                    log.info("STDERR: {}", el);
                });

        List<String> linesScenario = Files.readAllLines(scenario, Charset.forName("UTF-8"));

        String typeFrame = null;
        String charEndLine = null;

        StringBuilder stringBuilder = null;

        List<String> sendMessages = new ArrayList<>();
        List<String> receiveMessages = new ArrayList<>();


        for (int i = 0; i < linesScenario.size(); i++) {
            String line = linesScenario.get(i);

            if (line.charAt(0) == '@') {
log.info("{}", linesScenario.get(i));
                if (stringBuilder != null) {
                    String message = stringBuilder.toString();

                    if (typeFrame.equals("@send")) {

                        sendMessages.add(message);

                    } else if (typeFrame.equals("@receive")) {

                        receiveMessages.add(message);

                    }
                    stringBuilder = null;
                }

                String[] metaDataFrame = line.split(" ");

                typeFrame = metaDataFrame[0];

                String errorFlag = metaDataFrame[2];

                if (errorFlag.equals("witherror")) {

                    List<Byte> bytesEndLineList = Arrays.stream(metaDataFrame[4]
                                    .split("(?<=\\G.{2})"))
                            .map(el -> Byte.valueOf(Integer.valueOf(el, 16).byteValue()))
                            .collect(Collectors.toList());

                    byte[] bytesEndLineArray = new byte[bytesEndLineList.size()];
                    for (int k = 0; k < bytesEndLineList.size(); k++) {
                        bytesEndLineArray[k] = bytesEndLineList.get(k).byteValue();
                    }
                    try {
                        charEndLine = new String(bytesEndLineArray, "UTF-8").intern();
                    } catch (UnsupportedEncodingException ex) {
                        throw new CreateInstanseException("Encoding no support");
                    }


                } else {

                    List<Byte> bytesEndLineList = Arrays.stream(metaDataFrame[3]
                                    .split("(?<=\\G.{2})"))
                            .map(el -> Byte.valueOf(Integer.valueOf(el, 16).byteValue()))
                            .collect(Collectors.toList());

                    byte[] bytesEndLineArray = new byte[bytesEndLineList.size()];
                    for (int k = 0; k < bytesEndLineList.size(); k++) {
                        bytesEndLineArray[k] = bytesEndLineList.get(k).byteValue();
                    }
                    try {
                        charEndLine = new String(bytesEndLineArray, "UTF-8").intern();
                    } catch (UnsupportedEncodingException ex) {
                        throw new CreateInstanseException("Encoding no support");
                    }
                }

            } else {
                if (typeFrame == null || charEndLine == null) {
                    throw new CreateInstanseException(String.format("Error in line %d scenario %s", i + 1,
                            scenario.toAbsolutePath().toString()));
                }

                if (stringBuilder == null) {
                    stringBuilder = new StringBuilder();
                }
                stringBuilder.append(line.replaceAll("\r\n|\r|\n", "") + charEndLine);
            }
        }

        if (stringBuilder != null) {
            String message = stringBuilder.toString();

            if (typeFrame.equals("@send")) {

                sendMessages.add(message);

            } else if (typeFrame.equals("@receive")) {

                receiveMessages.add(message);

            }
            stringBuilder = null;
        }

        messageAmount = 0;

        flowMessagesFromAnalyzer
                .subscribe(mes -> {

                    messageAmount++;

                    if (messageAmount == 1) {
                        try {
                            flowMessagesForAnalyzer.tryEmitNext(receiveMessages.get(0).getBytes("UTF-8"));
                            log.info(receiveMessages.get(0).toString());
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    } else if (messageAmount == 2) {
                        try {
                            flowMessagesForAnalyzer.tryEmitNext(receiveMessages.get(1).getBytes("UTF-8"));
                            log.info(receiveMessages.get(1).toString());
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    } else if (messageAmount == 3) {
                        try {
                            flowMessagesForAnalyzer.tryEmitNext(receiveMessages.get(2).getBytes("UTF-8"));
                            log.info(receiveMessages.get(2).toString());
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }
}
