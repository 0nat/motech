package org.motechproject.batch.service;

import java.util.Date;

import org.motechproject.batch.exception.BatchException;
import org.motechproject.batch.model.JobExecutionHistoryList;
import org.springframework.stereotype.Service;

/**
 * Interface to perform the trigger operation for all types of jobs
 * @author Naveen
 *
 */
@Service
public interface JobTriggerService {

	/**
	 * trigger a job instantly
	 * @param jobName name of the job to be triggered
	 * @param date 
	 */
	void triggerJob(String jobName) throws BatchException;

	JobExecutionHistoryList getJObExecutionHistory(String jobName)
			throws BatchException;
 
}
