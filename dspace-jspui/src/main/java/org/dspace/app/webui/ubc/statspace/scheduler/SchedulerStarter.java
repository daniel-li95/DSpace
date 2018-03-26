package org.dspace.app.webui.ubc.statspace.scheduler;

import org.apache.log4j.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import static org.quartz.JobBuilder.newJob;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;

import org.quartz.SchedulerException;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import org.quartz.Trigger;
import static org.quartz.TriggerBuilder.newTrigger;
import org.quartz.impl.StdSchedulerFactory;

public class SchedulerStarter extends HttpServlet
{
    /** log4j logger */
    private static final Logger log = Logger.getLogger(SchedulerStarter.class);

	/** Autogenerated serial */
	private static final long serialVersionUID = 1738736327585377900L;

	private static int thumbnailJobIntervalInMinutes = 15; // TODO: CHANGE TO LONGER

	private Scheduler scheduler;

	/**
	 * Schedules the thumbnail job. This is a recurring job.
	 * @throws SchedulerException 
	 */
	private void runThumbnailJob() throws SchedulerException {
		// define the job and tie it to our HelloJob class
		JobDetail job = newJob(GenerateThumbnailJob.class)
				.withIdentity("job1", "group1")
				.build();
		
		// Trigger the job to run now, and then repeat
		Trigger trigger = newTrigger()
				.withIdentity("trigger1", "group1")
				.startNow()
				.withSchedule(simpleSchedule()
						.withIntervalInMinutes(thumbnailJobIntervalInMinutes)
						.repeatForever())
				.build();
		// Tell quartz to schedule the job using our trigger
		scheduler.scheduleJob(job, trigger);
	}

	/**
	 * Rebuild the Solr index from scratch on deployment. When we make changes
	 * to search, they're not reflected unless you run 
	 * `./bin/dspace index-discovery -b`
	 * This job basically runs this command only once on startup.
	 * @throws SchedulerException 
	 */
	private void runUpdateSolrJob() throws SchedulerException {
		JobKey jobKey = new JobKey("SolrIndexJob");
		JobDetail job = newJob(UpdateSolrIndexJob.class)
				.withIdentity(jobKey)
				.storeDurably() // required when not using a trigger
				.build();
		scheduler.addJob(job, true);
		// Trigger the job to run now
		scheduler.triggerJob(jobKey);
	}

	/** 
	 * Convenience method that can be overridden to do stuff when this servlet gets placed into service.
	 */
	@Override
	public void init() throws ServletException {
		try {
			scheduler = StdSchedulerFactory.getDefaultScheduler();
			scheduler.start();
			runUpdateSolrJob();
			runThumbnailJob();
		} catch (SchedulerException ex) {
			log.error(ex);
		}
	}

	/* (non-Javadoc)
	 * @see javax.servlet.GenericServlet#destroy()
	 */
	@Override
	public void destroy() {
		try {
			if (scheduler != null) {
				scheduler.shutdown();
			}
		} catch (SchedulerException ex) {
			log.error(ex);
		}
	}
	
}
