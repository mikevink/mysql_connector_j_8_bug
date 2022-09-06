package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MySQLController
{
    private final String startScriptPath;
    private final String stopScriptPath;
    private final Logger logger;

    public MySQLController(
        final String startScriptPath,
        final String stopScriptPath
    )
    {
        this.startScriptPath = startScriptPath;
        this.stopScriptPath = stopScriptPath;
        logger = LoggerFactory.getLogger(MySQLController.class.getSimpleName());
    }

    public void start() throws IOException, ExecutionException, InterruptedException
    {
        logger.info("Starting server");
        run(startScriptPath);
        logger.info("Started");
    }

    public void stop() throws IOException, ExecutionException, InterruptedException
    {
        logger.info("Stopping server");
        run(stopScriptPath);
        logger.info("Stopped");
    }

    private void run(final String scriptPath) throws InterruptedException, IOException, ExecutionException
    {
        final Process process = Runtime.getRuntime().exec(scriptPath);
        StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), logger::debug);
        Future<?> future = Executors.newSingleThreadExecutor().submit(streamGobbler);

        int exitCode = process.waitFor();
        assert exitCode == 0;

        future.get();
    }

    private static class StreamGobbler implements Runnable
    {
        private final InputStream inputStream;
        private final Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer)
        {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run()
        {
            new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
        }
    }
}
