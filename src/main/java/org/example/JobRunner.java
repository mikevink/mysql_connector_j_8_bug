package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import org.example.db.DSFactory;
import org.example.db.Dao;
import org.example.db.error.ESQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobRunner
{
    private final ScheduledExecutorService scheduler;
    private final String schemaPrefix;
    private final int numJobs;
    private final Logger logger;
    private List<Job> jobs;

    public JobRunner(final String schemaPrefix, final int numJobs, final int numCores)
    {
        this.schemaPrefix = schemaPrefix;
        this.numJobs = numJobs;
        scheduler = Executors.newScheduledThreadPool(numCores);
        logger = LoggerFactory.getLogger(JobRunner.class.getSimpleName());
    }

    public void init() throws ESQLException
    {
        final DataSource dataSource = DSFactory.create();
        final List<Dao> daos = IntStream.range(0, numJobs)
                                   .mapToObj(num -> new Dao(schemaPrefix + "_" + num, dataSource))
                                   .collect(Collectors.toList());
        for (Dao dao : daos)
        {
            dao.init();
        }
        jobs = daos.stream().map(dao -> new Job(dao, scheduler)).collect(Collectors.toList());
        logger.info("Initialisation complete");
    }

    public void start()
    {
        jobs.forEach(Job::schedule);
        logger.info("Jobs started");
    }

    @SuppressWarnings("BusyWait")
    public void waitForJobs(final JobState... states) throws InterruptedException
    {
        logger.info("Waiting for all jobs to be {}", Arrays.toString(states));
        final int target = jobs.size();
        final Freezer freezer = new Freezer(states);
        int count = 0;
        while (target != freezer.filterAndCount(jobs))
        {
            if (60 == count)
            {
                throw new RuntimeException(String.format(
                    "Not all jobs in expected states:\nExected states: %s\n%s",
                    Arrays.toString(states),
                    freezer
                ));
            }
            Thread.sleep(1000);
            count++;
        }
        logger.info("All jobs are {}", Arrays.toString(states));
    }

    public void printLastState()
    {
        final Freezer all = new Freezer(null);
        all.freeze(jobs);
        logger.info(all.toString());
        final Freezer disabled = new Freezer(new JobState[]{JobState.Disabled});
        disabled.freezeAndFilter(jobs);
        logger.info(disabled.toString("Disabled jobs (should be empty)"));
    }

    public void stop()
    {
        logger.info("Stopping the executor / jobs");
        jobs.forEach(Job::stop);
        scheduler.shutdown();
    }

    private static class Freezer
    {
        final Predicate<Job.FrozenJob> filter;
        private final List<Job.FrozenJob> frozen;

        public Freezer(final JobState[] states)
        {
            if (null == states)
            {
                filter = job -> true;
            }
            else
            {
                final HashSet<JobState> validStates = new HashSet<>(Arrays.asList(states));
                filter = job -> job.in(validStates);
            }
            frozen = new ArrayList<>();
        }

        public void freeze(final List<Job> jobs)
        {
            frozen.clear();
            frozen.addAll(jobs.stream().map(Job::freeze).collect(Collectors.toList()));
        }

        public List<Job.FrozenJob> freezeAndFilter(final List<Job> jobs)
        {
            frozen.clear();
            frozen.addAll(
                jobs.stream().map(Job::freeze).filter(filter).collect(Collectors.toList())
            );
            return frozen;
        }

        public long filterAndCount(final List<Job> jobs)
        {
            return freezeAndFilter(jobs).size();
        }

        public String toString()
        {
            return toString("Job States");
        }

        public String toString(final String tag)
        {
            return String.format(
                "%s:\n%s",
                tag,
                frozen.stream()
                    .map(Job.FrozenJob::toString)
                    .collect(Collectors.joining("\n"))
            );
        }
    }


}
