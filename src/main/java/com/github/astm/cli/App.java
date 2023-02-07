package com.github.astm.cli;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.github.astm.lib.TypeSideCommunication;
import com.github.astm.lib.exceptions.CreateInstanseException;
import com.github.astm.lib.tcp.TCP;
import com.github.astm.lib.tcp.TCPBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class App {

    private static Logger log = LoggerFactory.getLogger(App.class);

    private static Object obj = new Object();

    public static void main(String[] args) throws InterruptedException, CreateInstanseException {

        Path scenario;

        // requires -Dscenario=/path/tp/file
        if(System.getProperty("scenario") != null){
            scenario = Paths.get(System.getProperty("scenario"));
        } else {
            throw new CreateInstanseException("File scenario not found");
        }

        TCP serverTCPanalyzer = TCPBuilder
                                    .builder(TypeSideCommunication.SERVER, 8888)
                                    .setScenario(scenario)
                                    .build();

        serverTCPanalyzer.start();

        log.info("Server is started (port 8888)");

        synchronized(obj){
            obj.wait();
        }       
    }
} 
