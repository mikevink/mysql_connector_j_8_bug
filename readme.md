# Sample project to highlight MySQL Connector/J 8.0.30 bug

## tl;dr

When the MySQL server dies while queries are being run, the `MySQL Connector/J 8.0.30` occasionally 
fails with a NullPointerException (NPE), rather than a more useful CommunicationsException.
```java
java.lang.NullPointerException
	at com.mysql.cj.AbstractQuery.stopQueryTimer(AbstractQuery.java:228)
	at com.mysql.cj.jdbc.StatementImpl.stopQueryTimer(StatementImpl.java:643)
	at com.mysql.cj.jdbc.StatementImpl.executeQuery(StatementImpl.java:1182)
	at org.apache.commons.dbcp2.DelegatingStatement.executeQuery(DelegatingStatement.java:329)
	at org.apache.commons.dbcp2.DelegatingStatement.executeQuery(DelegatingStatement.java:329)
	at org.example.db.Dao.count(Dao.java:89)
	at org.example.Job.run(Job.java:52)
	at java.base/java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:515)
	at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
	at java.base/java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:304)
	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
	at java.base/java.lang.Thread.run(Thread.java:829)
```
This causes a problem for us as we cannot distinguish the NPE from any other non-sql errors and 
thus we no longer attempt to reconnect after a backoff period.

When running the same code with `MySQL Connector/J 5.1.49`, all errors are handled as expected.