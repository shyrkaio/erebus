package io.github.shyrkaio.hypnos;

import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

//@TODO rewrite this path to regular path (according to kubernetes practice)
@Path("/erebus/v1alpha1/hypnos")
public class HypnosStatus {

    @Inject
    org.quartz.Scheduler quartz;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<JobExecutionContext> hello() throws SchedulerException {
        List<JobExecutionContext> currentlyExecutingJobs = quartz.getCurrentlyExecutingJobs();
        return currentlyExecutingJobs;
    }
    @GET
    @Path("/summary")
    @Produces(MediaType.TEXT_PLAIN)
    public String summary() throws SchedulerException {
        return quartz.getMetaData().getSummary();
    }


}