package io.github.shyrkaio.hypnos.crd;

import io.fabric8.kubernetes.client.*;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.quarkus.runtime.StartupEvent;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@ApplicationScoped
public class HypnosWatcher {
    private static Logger _log = Logger.getLogger(HypnosWatcher.class.getCanonicalName());

    @Inject
    KubernetesClient client;

    @Inject
    org.quartz.Scheduler quartz;

    //memorycache
    //@TODO store those with quartzjob
    private final Map<String, Hypnos> cache = new ConcurrentHashMap<>();

    //
    private Executor executor = Executors.newSingleThreadExecutor();

    void onStartup(@Observes StartupEvent _ev) {

        try {
            _log.log(Level.INFO, " scheduler shyrkaio.github.io is Started : "+quartz.isStarted());
            _log.log(Level.INFO, " scheduler shyrkaio.github.io is InStandbyMode "+quartz.isInStandbyMode());
        } catch (SchedulerException e) {
            _log.log(Level.SEVERE, this.getClass() + " crash ", e);
            System.exit(-1);
        }
        new Thread(this::runWatch).start();
    }

    private void runWatch() {
        this.listThenWatch(this::handleEvent);
    }


    private void handleEvent(Watcher.Action action, String uid) {
            try {
                Hypnos resource = cache.get(uid);
                if (resource == null) {
                    return;
                }
            } catch (Exception e) {
                _log.log(Level.SEVERE, this.getClass() + " crash ", e);
                System.exit(-1);
            }

    }

    public void listThenWatch(BiConsumer<Watcher.Action, String> callback)  {
        CustomResourceDefinitionContext crdDefinitionContext = new CustomResourceDefinitionContext.Builder()
                    .withVersion("v1alpha3")
                    .withScope("Cluster")
                    .withGroup("shyrkaio.github.io")
                    .withPlural("hypnox")
                    .build();

        Watcher<Hypnos> watchHypnox =  new Watcher<Hypnos>() {

            @Override
            public boolean reconnecting() {
                return Watcher.super.reconnecting();
            }

            @Override
            public void eventReceived(Action action, Hypnos hypnos) {
                try {
                    String uid = hypnos.getMetadata().getUid();
                    if (cache.containsKey(uid)) {
                        int knownResourceVersion = Integer.parseInt(cache.get(uid).getMetadata().getResourceVersion());
                        int receivedResourceVersion = Integer.parseInt(hypnos.getMetadata().getResourceVersion());
                        if (knownResourceVersion > receivedResourceVersion) {
                            _log.warning("received " + action + " for " +hypnos.getMetadata().getName()+" but knownResourceVersion > receivedResourceVersion ");
                            return;
                        }
                    }
                    _log.fine("Received " + action + " for resource " + hypnos);
                    if (action == Action.ADDED) {
                        _log.info("Added " + action + " for resource " + hypnos);
                        cache.put(uid, hypnos);
                        handleAddedEvent(action, hypnos);

                    } else if (action == Action.MODIFIED) {

                        _log.info("Modified " + action + " for resource " + hypnos);
                        cache.put(uid, hypnos);
                        handleModifiedEvent(action, hypnos);
                    } else if (action == Action.DELETED) {
                        _log.info("Deleted " + action + " for resource " + hypnos);
                        cache.remove(uid);
                        handleRemovedEvent(action, hypnos);
                    } else {
                        _log.warning("Received unexpected " + action + " event for " + hypnos);
                        System.exit(-1);
                    }
                    executor.execute(() -> callback.accept(action, uid));
                } catch (Exception e) {
                    _log.log(Level.SEVERE, this.getClass() + " is closed ", e);
                    System.exit(-1);
                }
            }

            @Override
            public void onClose() {
                Watcher.super.onClose();
            }

            @Override
            public void onClose(WatcherException cause) {
                if (cause != null) {
                    _log.info(this.getClass() + " is closed because :{" + cause.asClientException().getCode() + ":" + cause.asClientException().getStatus() + "}");
                }else{
                    _log.warning(this.getClass() + " is closed without a cause ");
                }
            }
        };

        client.customResources(Hypnos.class, HypnosList.class).watch(watchHypnox);

    }

    private void handleModifiedEvent(Watcher.Action action, Hypnos hypnos) {
        _log.fine("Modified " + action + " for resource " + hypnos);
        handleRemovedEvent(action,hypnos);
        handleAddedEvent(action,hypnos);
    }

    private void handleRemovedEvent(Watcher.Action action, Hypnos hypnos) {
        _log.fine("Removed " + action + " for resource " + hypnos);

        Trigger wakeupTrigger = newTrigger()
                .withIdentity(hypnos.getMetadata().getName()+"-wakeup", "hypnos.shyrkaio.github.io")
                .withSchedule(cronSchedule(cronScheduleConvert(hypnos.getSpec().getWakeupCron())))
                .build();
        Trigger sleepTrigger = newTrigger()
                .withIdentity(hypnos.getMetadata().getName()+"-sleep", "hypnos.shyrkaio.github.io")
                .withSchedule(cronSchedule(cronScheduleConvert(hypnos.getSpec().getSleepCron())))
                .build();

        try {
            quartz.unscheduleJob(wakeupTrigger.getKey());
            quartz.unscheduleJob(sleepTrigger.getKey());
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    private void handleAddedEvent(Watcher.Action action, Hypnos hypnos) {
        _log.fine("Added " + action + " for resource " + hypnos);

        //@todo Check that the job exists in quartz
        JobDetail wakeupJob = newJob(HypnosJob.class)
                .withIdentity(hypnos.getMetadata().getName()+"-wakeup", "hypnos.shyrkaio.github.io")
                .build();

        Trigger wakeupTrigger = newTrigger()
                .withIdentity(hypnos.getMetadata().getName()+"-wakeup", "hypnos.shyrkaio.github.io")
                .withSchedule(cronSchedule(cronScheduleConvert(hypnos.getSpec().getWakeupCron())))
                .build();

        configureJob(hypnos, wakeupJob,HypnosJobAction.ACTION_CRON_WAKEUP);



        JobDetail sleepJob = newJob(HypnosJob.class)
                .withIdentity(hypnos.getMetadata().getName()+"-sleep", "hypnos.shyrkaio.github.io")
                .build();

        configureJob(hypnos, sleepJob,HypnosJobAction.ACTION_CRON_SLEEP);

        Trigger sleepTrigger = newTrigger()
                .withIdentity(hypnos.getMetadata().getName()+"-sleep", "hypnos.shyrkaio.github.io")
                .withSchedule(cronSchedule(cronScheduleConvert(hypnos.getSpec().getSleepCron())))
                .build();
        try {

            quartz.scheduleJob(wakeupJob, wakeupTrigger);
            quartz.scheduleJob(sleepJob, sleepTrigger);
        } catch (SchedulerException e) {
            _log.log(Level.WARNING, this.getClass() + " error while scheduling ", e);
        }

    }

    // this is a temporary hack in order to use "cron guru https://crontab.guru/" syntaxe.
    //https://www.freeformatter.com/cron-expression-generator-quartz.html
    private String cronScheduleConvert(String cron) {
        String[] cronVal = cron.split(" ");
        if(cronVal[2].equals(cronVal[3]) && cronVal[3].equals("*")){
            cronVal[2]="?";
        }
        String quartzcronval="5 "+String.join(" ", cronVal)+"";
        _log.fine("quartzcronval : "+quartzcronval +"# check at https://www.freeformatter.com/cron-expression-generator-quartz.html");
        return quartzcronval;
    }

    private HypnosWatcher configureJob(Hypnos hypnos, JobDetail wakeupJob, String jobAction) {
        wakeupJob.getJobDataMap().put(HypnosJob.NAMESPACE_TARGETED_LABEL, hypnos.getSpec().getNamespaceTargetedLabel());
        wakeupJob.getJobDataMap().put(HypnosJob.TARGETED_LABEL,hypnos.getSpec().getTargetedLabel());
        wakeupJob.getJobDataMap().put(HypnosJob.RESOURCE_TYPE, String.join(", ", hypnos.getSpec().getResourceType()));
        wakeupJob.getJobDataMap().put(HypnosJob.ACTION_CRON, jobAction);
        wakeupJob.getJobDataMap().put(HypnosJob.HYPNOS_NAME, hypnos.getMetadata().getName());
        //

        if("wakeup".equals(jobAction)){
            wakeupJob.getJobDataMap().put(HypnosJob.DEFINED_CRON, hypnos.getSpec().getWakeupCron());
        }else if("sleep".equals(jobAction)) {
            wakeupJob.getJobDataMap().put(HypnosJob.DEFINED_CRON, hypnos.getSpec().getSleepCron());
        }else{
            _log.log(Level.WARNING, "This should never happen, yet the action is not recognised (action : " + jobAction+")");
        }
        return this;
    }
}
