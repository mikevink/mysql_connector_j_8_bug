package org.example;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.example.db.Dao;
import org.example.db.error.ESQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Job implements Runnable
{

    private final Dao dao;
    private final ScheduledExecutorService scheduler;
    private final AtomicReference<JobState> state;
    private final AtomicReference<ScheduledFuture<?>> schedule;
    private final AtomicBoolean enabled;
    private final Logger logger;

    public Job(final Dao dao, final ScheduledExecutorService scheduler)
    {
        this.dao = dao;
        this.scheduler = scheduler;
        state = new AtomicReference<>(JobState.Disconnected);
        schedule = new AtomicReference<>(null);
        enabled = new AtomicBoolean(true);
        logger = LoggerFactory.getLogger(dao.schema + "[job]");
    }

    public void schedule()
    {
        if (enabled.get())
        {
            schedule.set(scheduler.schedule(this, 500, TimeUnit.MILLISECONDS));
        }
    }

    @Override
    public void run()
    {
        final String oldName = maybeSetThreadName(dao.schema);
        try
        {
            state.compareAndSet(JobState.Disconnected, JobState.Running);
            logger.trace("Starting Job");
            logger.debug("Row Count: {}", dao.count());

            dao.insert("run");
            if (JobState.Running == state.get())
            {
                schedule();
            }
        }
        catch (ESQLException e)
        {
            if (e.isConnectionRelated)
            {
                e.log(logger, "Connection error encountered");
                state.compareAndSet(JobState.Running, JobState.Disconnected);
            }
            else if (e.isTimeout)
            {
                e.log(logger, "Connection timeout encountered");
                state.compareAndSet(JobState.Running, JobState.Timeout);
            }
            else
            {
                e.log(logger, "SQLException encountered, disabling job");
                state.set(JobState.Disabled);
            }
        }
        catch (final Exception catchAll)
        {
            logger.error("Non-SQL Exception encountered, disabling job", catchAll);
            state.set(JobState.Disabled);
        }
        finally
        {
            logger.trace("Finished Job");
            maybeSetThreadName(oldName);
        }
    }

    public FrozenJob freeze()
    {
        return new FrozenJob(dao.schema, state.get());
    }

    public void stop()
    {
        enabled.set(false);
        Optional.ofNullable(schedule.get()).ifPresent(s -> s.cancel(true));
    }

    private String maybeSetThreadName(final String newName)
    {
        final Thread currentThread = Thread.currentThread();
        final String oldName = currentThread.getName();
        if (!oldName.equals(newName))
        {
            currentThread.setName(newName);
        }
        return oldName;
    }

    public static class FrozenJob
    {
        private final String name;
        private final JobState state;

        public FrozenJob(final String name, final JobState state)
        {
            this.name = name;
            this.state = state;
        }

        public boolean in(final Set<JobState> states)
        {
            return states.contains(this.state);
        }

        @Override
        public String toString()
        {
            return String.format("%s::%s", name, state);
        }
    }
}
