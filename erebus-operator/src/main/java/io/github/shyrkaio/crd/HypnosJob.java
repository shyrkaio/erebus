package io.github.shyrkaio.crd;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import org.quartz.*;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class HypnosJob implements Job {
    private static Logger _log = Logger.getLogger(HypnosJob.class.getCanonicalName());

    //@Inject
    private KubernetesClient client;

    CustomResourceDefinitionContext crdDefinitionContext = new CustomResourceDefinitionContext.Builder()
            .withVersion("v1alpha1")
            .withScope("Cluster")
            .withGroup("shyrkaio.github.io")
            .withPlural("hypnox")
            .build();

    private KubernetesClient getClient(){
        if (client == null){
            _log.warning("client Should not ne null, this is loading crap that @k should correct");
            client = makeDefaultClient();
        }
        return client;
    }


    public static final String NAMESPACE_TARGETED_LABEL = "namespaceTargetedLabel";
    public static final String TARGETED_LABEL = "targetedLabel";
    public static final String RESOURCE_TYPE = "resourceType";
    public static final String ACTION_CRON = "action-cron";
    public static final String DEFINED_CRON = "defined-cron";

    public static final String HYPNOS_NAME = "hypnosName";



    //
    public static final String EXECUTION_COUNT = "count";

    // Since Quartz will re-instantiate a class every time it
    // gets executed, members non-static member variables can
    // not be used to maintain state!
    private int _counter = 1;

    //
    KubernetesClient makeDefaultClient() {
        String currentNS = System.getProperty("io.shyrka.hypnos.ns","");
        if (!currentNS.isBlank()){
            new DefaultKubernetesClient().inNamespace(currentNS);
        }

        File serviceAccountNamespace = new File("/var/run/secrets/kubernetes.io/serviceaccount/namespace");
        if (serviceAccountNamespace.exists() && serviceAccountNamespace.isFile()){
            try {
                currentNS = new String(Files.readAllBytes(Paths.get(serviceAccountNamespace.getPath())));
                new DefaultKubernetesClient().inNamespace(currentNS);
            } catch (IOException e) {

                _log.log(Level.SEVERE, this.getClass() + " error while scheduling ", e);
                new DefaultKubernetesClient().inNamespace("default");
            }
        }
        _log.warning("no value define for current Namespace");

        return new DefaultKubernetesClient().inNamespace("default");
    }

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

        //List<String> numbers = Arrays.asList("How", "To", "Do", "In", "Java");
        //String[] resourceType = resourceTypeString.split(",");
        int count = data.getInt(EXECUTION_COUNT);

        //
        _log.info("HypnosJob: " + jobKey + " executing at " + new Date() + "\n" +
                "  namespaceTargetedLabel is " + namespaceTargetedLabel + "\n" +
                "  targetedLabel is " + targetedLabel + "\n" +
                "  resourceTypeString is " + resourceTypeString + "\n" +
                "  actionCron is " + actionCron + "\n" +
                "  definedCron is " + definedCron + "\n" +
                "  execution count (from job map) is " + count + "\n" +
                "  execution count (from job member variable) is " + _counter);


        count++;
        client=getClient();
        data.put(EXECUTION_COUNT, count);
        if(resourceTypeString.contains("Deployment")){
            cronActionTargetedDeployments(namespaceTargetedLabel, targetedLabel, hypnosName,actionCron);
        }

        _counter++;
        //
    }

    private Hypnos getHypnos(String hypnosName) {
        client=getClient();
        KubernetesDeserializer.registerCustomKind("io.github.shyrkaio.operator/v1alpha1", "Hypnos",
                Hypnos.class);



        Hypnos hypnos = client.customResources(crdDefinitionContext,
                Hypnos.class,
                HypnosList.class,
                HypnosDoneable.class).withName(hypnosName).get();
        _log.fine("get hypnos named "+hypnosName);
        return hypnos;
    }

    private void cronActionTargetedDeployments(String namespaceTargetedLabel, String targetedLabel, String hypnosName,String action){

        NamespaceList myNs = getNamespaceList(namespaceTargetedLabel);
        if (myNs ==null){
            _log.warning("no Namespace with label "+namespaceTargetedLabel);
            return;
        }
        for (Namespace ns : myNs.getItems()) {
            List<Deployment> deploys = getDeploymentList(client, ns, targetedLabel);
            for (Deployment dep : deploys) {
                cronActionDeployment(ns, dep, hypnosName, action);
            }
        }
}

    private void cronActionDeployment(Namespace ns, Deployment dep, String hypnosName, String actionCron) {
        
        String depName = dep.getMetadata().getName();
        Integer replicas = dep.getSpec().getReplicas();

        String sDate = String.valueOf(LocalDateTime.now());

        Map<String, String> annotations = dep.getMetadata().getAnnotations();
        HypnosStatusEvent hypnosStatusEvent = null;

        if ("wakeup".equals(actionCron)) {

            if(replicas == 0) {
                replicas = Integer.valueOf(annotations.getOrDefault("io.shyrka.erebus.hypnos/replicas", "1"));
            }
            sDate = annotations.put("io.shyrka.erebus.hypnos/awaken-at", sDate);
            dep.getSpec().setReplicas(replicas);
            hypnosStatusEvent = new HypnosStatusEvent(ns.getMetadata().getName(), "wakeup", depName, "Deployment", replicas);


        }
        if ("sleep".equals(actionCron)){
            if(replicas>0) {
                String put = annotations.put("io.shyrka.erebus.hypnos/replicas", String.valueOf(replicas));
            }
            sDate = annotations.put("io.shyrka.erebus.hypnos/stop-at", sDate);
            dep.getSpec().setReplicas(0);
            hypnosStatusEvent = new HypnosStatusEvent(ns.getMetadata().getName(), "sleep", depName, "Deployment", replicas);

        }
        client.apps().deployments().inNamespace(ns.getMetadata().getName()).createOrReplace(dep);

        _log.info("HypnosJob { Deploy : "+depName+" "+actionCron+" at "+sDate+"}");
        if(_log.isLoggable(Level.FINE)) {
            if(hypnosStatusEvent == null){
            _log.fine(hypnosStatusEvent.toString());
            }
        }
    }

    private NamespaceList getNamespaceList(String namespaceTargetedLabel) {

        if("".equals(namespaceTargetedLabel)){
            _log.warning("namespaceTargetedLabel is undefined, so using 'default'");
            namespaceTargetedLabel="default";
        }
        NamespaceList myNs = client.namespaces().withLabel(namespaceTargetedLabel).list();
        if(_log.isLoggable(Level.FINE)) {
                        _log.fine("HypnosJob: { nbNamespace :" + myNs.getItems().size() + " with " + namespaceTargetedLabel + "}");
        }
        return myNs;
    }

    private List<Deployment> getDeploymentList(KubernetesClient client, Namespace ns, String targetedLabel) {
        String nsName = ns.getMetadata().getName();
        List<Deployment> deploys;
        if("".equals(targetedLabel)){
            _log.warning("targetedLabel is undefined, so select all Deployment");
            deploys = client.apps().deployments().inNamespace(nsName).list().getItems();
        }else{
            deploys = client.apps().deployments().inNamespace(nsName).withLabel(targetedLabel).list().getItems();
        }
            if(_log.isLoggable(Level.FINE)) {
                _log.fine("HypnosJob: { nbDeploys :" + deploys.size() + " with " + targetedLabel + " in "+nsName+"}");
            }
            return deploys;
    }
}
