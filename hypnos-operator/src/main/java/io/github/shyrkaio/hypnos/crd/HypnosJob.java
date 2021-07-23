package io.github.shyrkaio.hypnos.crd;

import org.quartz.*;

import javax.enterprise.context.ApplicationScoped;
import java.util.logging.Logger;

@ApplicationScoped
public class HypnosJob implements Job {
    private static Logger _log = Logger.getLogger(HypnosJob.class.getCanonicalName());

    public static final String NAMESPACE_TARGETED_LABEL = "namespaceTargetedLabel";
    public static final String TARGETED_LABEL = "targetedLabel";
    public static final String RESOURCE_TYPE = "resourceType";
    public static final String ACTION_CRON = "action-cron";
    public static final String DEFINED_CRON = "defined-cron";

    public static final String HYPNOS_NAME = "hypnosName";


    /**
     * Empty constructor for job initialization
     */
    public HypnosJob() {
    }

    /**
     * <p>
     * Called by the <code>{@link org.quartz.Scheduler}</code> when a
     * <code>{@link org.quartz.Trigger}</code> fires that is associated with
     * the <code>Job</code>.
     * </p>
     *
     * @throws JobExecutionException
     *             if there is an exception while executing the job.
     */
    public void execute(JobExecutionContext context)
            throws JobExecutionException {

        // This job simply prints out its job name and the
        // date and time that it is running
        JobKey jobKey = context.getJobDetail().getKey();

        // Grab and print passed parameters
        JobDataMap data = context.getJobDetail().getJobDataMap();
        String namespaceTargetedLabel = data.getString(NAMESPACE_TARGETED_LABEL);
        String targetedLabel = data.getString(TARGETED_LABEL);
        String hypnosName = data.getString(HYPNOS_NAME);
        String actionCron = data.getString(ACTION_CRON);
        String definedCron = data.getString(DEFINED_CRON);

        String resourceTypeString = data.getString(RESOURCE_TYPE);

        HypnosJobAction action = new HypnosJobAction()
                .setNamespaceTargetedLabel(namespaceTargetedLabel)
                .setTargetedLabel(targetedLabel)
                .setResourceTypeString(resourceTypeString)
                .setHypnosName(hypnosName)
                .setActionCron(actionCron)
                .setDefinedCron(definedCron);


        //
        _log.info(action.getMsg(jobKey.toString()));

        if(resourceTypeString.contains("Deployment")){
            action.cronActionTargetedDeployments(namespaceTargetedLabel, targetedLabel, hypnosName,actionCron);
        }

        if(resourceTypeString.contains("StatefulSet")){
            action.cronActionTargetedStatefulSet(namespaceTargetedLabel, targetedLabel, hypnosName,actionCron);
        }

        if(resourceTypeString.contains("DeploymentConfig")){
            _log.warning("resourceType DeploymentConfig is need roles with  - apiGroups: [\"apps.openshift.io\"] resources: [\"deploymentconfigs\"] verbs: [\"create\", \"patch\", \"list\", \"get\", \"update\"]");
            HypnosDeploymentConfigJobAction oAction = new HypnosDeploymentConfigJobAction()
                    .setNamespaceTargetedLabel(namespaceTargetedLabel)
                    .setTargetedLabel(targetedLabel)
                    .setHypnosName(hypnosName)
                    .setActionCron(actionCron)
                    .setDefinedCron(definedCron);
            oAction.cronActionTargetedDeploymentConfigs(namespaceTargetedLabel, targetedLabel, hypnosName,actionCron);
        }

        if(!resourceTypeString.contains("Deployment") && !resourceTypeString.contains("StatefulSet") && resourceTypeString.contains("DeploymentConfig")){
            _log.warning("resourceType " +resourceTypeString+ " do not exist, this case should never happen");
        }

    }

}
