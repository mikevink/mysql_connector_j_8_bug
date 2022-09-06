package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.example.db.error.ESQLException;

public class Main
{
    public static void main(String[] args) throws ESQLException, InterruptedException, IOException, ExecutionException
    {
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "yyyy-MM-dd HH:mm:ss.S");
        final JobRunner runner = new JobRunner("sqltest", 30, 10);
        runner.init();
        runner.start();
        runner.waitForJobs(JobState.Running);
        Thread.sleep(5000);
        killMysql();
        runner.waitForJobs(JobState.Disabled, JobState.Disconnected, JobState.Timeout);
        runner.stop();
        runner.printLastState();
        // can't work out why the above _stop_ isn't killing everything, so force an exit
        System.exit(0);
    }

    private static void killMysql() throws InterruptedException, IOException, ExecutionException
    {
        // replace this bit with whatever can kill a mysql server on your end
        final String homeDirectory = System.getProperty("user.home");
        final String mysqlKillScript = String.format("%s/bin/killMysql.sh", homeDirectory);
        // this ough to (mostly) work
        final Process process = Runtime.getRuntime().exec(mysqlKillScript);
        StreamGobbler streamGobbler =
            new StreamGobbler(process.getInputStream(), System.out::println);
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