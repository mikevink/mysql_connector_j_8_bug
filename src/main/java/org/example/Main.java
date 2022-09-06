package org.example;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.example.db.error.ESQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main
{
    public static void main(String[] args) throws ESQLException, InterruptedException, IOException, ExecutionException
    {
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "yyyy-MM-dd HH:mm:ss.S");
        final Logger logger = LoggerFactory.getLogger(Main.class.getSimpleName());
        final MySQLController mySQLController = mySQLController();
        mySQLController.start();
        final JobRunner runner = new JobRunner("sqltest", 30, 10);
        runner.init();
        runner.start();
        runner.waitForJobs(JobState.Running);
        logger.info("Sleeping for 5s");
        Thread.sleep(5000);
        mySQLController.stop();
        runner.waitForJobs(JobState.Disabled, JobState.Disconnected, JobState.Timeout);
        runner.stop();
        runner.printLastState();
        // can't work out why the above _stop_ isn't killing everything, so force an exit
        System.exit(0);
    }

    // replace the (start|stop)ScriptPaths here with your own scripts to control the mysql server
    // for best results, scripts ought to be idempotent
    private static MySQLController mySQLController()
    {
        final String homeDirectory = System.getProperty("user.home");
        final String startScriptPath = String.format("%s/bin/startMySQL.sh", homeDirectory);
        final String stopScriptPath = String.format("%s/bin/stopMySQL.sh", homeDirectory);
        return new MySQLController(startScriptPath, stopScriptPath);
    }
}