package io.github.shyrkaio.hypnos.crd;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class HypnosJobAction {
    private static Logger _log = Logger.getLogger(HypnosJobAction.class.getCanonicalName());


    private KubernetesClient client;

    CustomResourceDefinitionContext crdDefinitionContext = new CustomResourceDefinitionContext.Builder()
            .withVersion("v1alpha1")
            .withScope("Cluster")
            .withGroup("shyrkaio.github.io")
            .withPlural("hypnox")
            .build();

    private KubernetesClient getClient(){
        if (client == null){
            client = makeDefaultClient();
        }
        return client;
    }

    //@TODO change that to enum
    public static final String NAMESPACE_TARGETED_LABEL = "namespaceTargetedLabel";
    public static final String TARGETED_LABEL = "targetedLabel";
    public static final String RESOURCE_TYPE = "resourceType";
    public static final String ACTION_CRON = "action-cron";
    public static final String DEFINED_CRON = "defined-cron";
    public static final String ACTION_CRON_WAKEUP = "wakeup";
    public static final String ACTION_CRON_SLEEP = "sleep";

    public static final String HYPNOS_NAME = "hypnosName";

    private String namespaceTargetedLabel = "";
    private String targetedLabel = "";
    private String hypnosName = "";
    private String actionCron = "";
    private String definedCron = "";

    private String resourceTypeString = "";


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
    public HypnosJobAction() {
    }

    public String getMsg(String jobKey) {
        return "HypnosJob: " + jobKey + " executing at " + new Date() + "\n" +
                "  namespaceTargetedLabel is " + getNamespaceTargetedLabel() + "\n" +
                "  targetedLabel is " + getTargetedLabel() + "\n" +
                "  resourceTypeString is " + getResourceTypeString() + "\n" +
                "  actionCron is " + getActionCron() + "\n" +
                "  definedCron is " + getDefinedCron() + "\n";
    }

    private Hypnos getHypnos(String hypnosName) {
        KubernetesDeserializer.registerCustomKind("io.github.shyrkaio.operator/v1alpha1", "Hypnos",
                Hypnos.class);



        Hypnos hypnos = getClient().customResources(Hypnos.class,
                HypnosList.class
                ).withName(hypnosName).get();
        _log.fine("get hypnos named "+hypnosName);
        return hypnos;
    }

    public List<HypnosStatusEvent> cronActionTargetedDeployments(String namespaceTargetedLabel, String targetedLabel, String hypnosName,
            String action) {
       List<HypnosStatusEvent> eventsLst = new ArrayList<>();
        NamespaceList myNs = getNamespaceList(namespaceTargetedLabel);
        if (myNs == null) {
            _log.warning("no Namespace with label " + namespaceTargetedLabel);
            return eventsLst;
        }
        for (Namespace ns : myNs.getItems()) {
            List<Deployment> deploys = getDeploymentList(ns, targetedLabel);
            for (Deployment dep : deploys) {
                HypnosStatusEvent event = cronActionDeployment(ns, dep, hypnosName, action);
                eventsLst.add(event);
            }
        }
        return eventsLst;
    }

    private HypnosStatusEvent cronActionDeployment(Namespace ns, Deployment dep, String hypnosName, String actionCron) {
        
        String depName = dep.getMetadata().getName();
        Integer replicas = dep.getSpec().getReplicas();

        String sDate = String.valueOf(LocalDateTime.now());

        Map<String, String> annotations = dep.getMetadata().getAnnotations();
        if(annotations==null){
            annotations = new HashMap<>();
        }
        HypnosStatusEvent hypnosStatusEvent = null;

        if ("wakeup".equals(actionCron)) {

            if(replicas == 0) {
                replicas = Integer.valueOf(annotations.getOrDefault("io.shyrka.erebus.hypnos/replicas", "1"));
            }
            annotations.put("io.shyrka.erebus.hypnos/awaken-at", sDate);
            dep.getSpec().setReplicas(replicas);
            hypnosStatusEvent = new HypnosStatusEvent(ns.getMetadata().getName(), "wakeup", depName, "Deployment", replicas);


        }
        if ("sleep".equals(actionCron)){
            if(replicas>0) {
                String put = annotations.put("io.shyrka.erebus.hypnos/replicas", String.valueOf(replicas));
            }
            annotations.put("io.shyrka.erebus.hypnos/stop-at", sDate);
            dep.getSpec().setReplicas(0);
            hypnosStatusEvent = new HypnosStatusEvent(ns.getMetadata().getName(), "sleep", depName, "Deployment", replicas);

        }
        getClient().apps().deployments().inNamespace(ns.getMetadata().getName()).createOrReplace(dep);

        _log.info("HypnosJob { Deploy : "+depName+" "+actionCron+" at "+sDate+"}");
        if(_log.isLoggable(Level.FINE)) {
            if(hypnosStatusEvent == null){
            _log.fine("hypnosStatusEvent is null");
            }
        }
        return hypnosStatusEvent;
    }

    public List<HypnosStatusEvent> cronActionTargetedStatefulSet(String namespaceTargetedLabel, String targetedLabel, String hypnosName, String action) {
        List<HypnosStatusEvent> eventsLst = new ArrayList<>();
        NamespaceList myNs = getNamespaceList(namespaceTargetedLabel);
        if (myNs == null) {
            _log.warning("no Namespace with label " + namespaceTargetedLabel);
            return eventsLst;
        }
        for (Namespace ns : myNs.getItems()) {
            List<StatefulSet> statefulSets = getStatefulSetList(ns, targetedLabel);
            for (StatefulSet sts : statefulSets) {
                HypnosStatusEvent event = cronActionStatefulSet(ns, sts, hypnosName, action);
                eventsLst.add(event);
            }
        }
        return eventsLst;
    }

    private HypnosStatusEvent cronActionStatefulSet(Namespace ns, StatefulSet sts, String hypnosName, String action) {
        String depName = sts.getMetadata().getName();
        Integer replicas = sts.getSpec().getReplicas();

        String sDate = String.valueOf(LocalDateTime.now());

        Map<String, String> annotations = sts.getMetadata().getAnnotations();
        if(annotations==null){
            annotations = new HashMap<>();
        }
        HypnosStatusEvent hypnosStatusEvent = null;

        if ("wakeup".equals(actionCron)) {

            if(replicas == 0) {
                replicas = Integer.valueOf(annotations.getOrDefault("io.shyrka.erebus.hypnos/replicas", "1"));
            }
            annotations.put("io.shyrka.erebus.hypnos/awaken-at", sDate);
            sts.getSpec().setReplicas(replicas);
            hypnosStatusEvent = new HypnosStatusEvent(ns.getMetadata().getName(), "wakeup", depName, "Deployment", replicas);


        }
        if ("sleep".equals(actionCron)){
            if(replicas>0) {
                String put = annotations.put("io.shyrka.erebus.hypnos/replicas", String.valueOf(replicas));
            }
            annotations.put("io.shyrka.erebus.hypnos/stop-at", sDate);
            sts.getSpec().setReplicas(0);
            hypnosStatusEvent = new HypnosStatusEvent(ns.getMetadata().getName(), "sleep", depName, "Deployment", replicas);

        }
        getClient().apps().statefulSets().inNamespace(ns.getMetadata().getName()).createOrReplace(sts);

        _log.info("HypnosJob: { name: "+this.getHypnosName()+", StatefulSets : "+depName+" "+actionCron+" at "+sDate+"}");
        if(_log.isLoggable(Level.FINE)) {
            if(hypnosStatusEvent == null){
                _log.fine("hypnosStatusEvent is null");
            }
        }
        return hypnosStatusEvent;
    }

    private List<StatefulSet> getStatefulSetList(Namespace ns, String targetedLabel) {
        String nsName = ns.getMetadata().getName();
        List<StatefulSet> sts;
        if("".equals(targetedLabel)){
            _log.warning("targetedLabel is undefined, so select all StatefulSet");
            sts = getClient().apps().statefulSets().inNamespace(nsName).list().getItems();
        }else{
            sts = getClient().apps().statefulSets().inNamespace(nsName).withLabel(targetedLabel).list().getItems();
        }
        if(_log.isLoggable(Level.FINE)) {
            _log.fine("HypnosJob:  { name: "+this.getHypnosName()+", nbStatefulSet :" + sts.size() + " with " + targetedLabel + " in "+nsName+"}");
        }
        return sts;
    }

    private NamespaceList getNamespaceList(String namespaceTargetedLabel) {

        if("".equals(namespaceTargetedLabel)){
            _log.warning("namespaceTargetedLabel is undefined, so using 'default'");
            namespaceTargetedLabel="default";
        }
        NamespaceList myNs = getClient().namespaces().withLabel(namespaceTargetedLabel).list();
        if(myNs.getItems().isEmpty()){
            _log.warning("No Namespace as "+namespaceTargetedLabel+"' as label ");
            _log.finer("try kubectl get ns -l "+namespaceTargetedLabel);
        }
        if(_log.isLoggable(Level.FINE)) {
                        _log.fine("HypnosJob:  { name: "+this.getHypnosName()+", nbNamespace :" + myNs.getItems().size() + " with " + namespaceTargetedLabel + "}");
        }
        return myNs;
    }

    private List<Deployment> getDeploymentList(Namespace ns, String targetedLabel) {
        String nsName = ns.getMetadata().getName();
        List<Deployment> deploys;
        if("".equals(targetedLabel)){
            _log.warning("targetedLabel is undefined, so select all Deployment");
            deploys = getClient().apps().deployments().inNamespace(nsName).list().getItems();
        }else{
            deploys = getClient().apps().deployments().inNamespace(nsName).withLabel(targetedLabel).list().getItems();
        }
            if(_log.isLoggable(Level.FINE)) {
                _log.fine("HypnosJob: { name: "+this.getHypnosName()+", nbDeploys :" + deploys.size() + " with " + targetedLabel + " in "+nsName+"}");
            }
            return deploys;
    }

    public String getNamespaceTargetedLabel() {
        return namespaceTargetedLabel;
    }

    public HypnosJobAction setNamespaceTargetedLabel(String namespaceTargetedLabel) {
        this.namespaceTargetedLabel = namespaceTargetedLabel;
        return this;
    }

    public String getTargetedLabel() {
        return targetedLabel;
    }

    public HypnosJobAction setTargetedLabel(String targetedLabel) {
        this.targetedLabel = targetedLabel;
        return this;
    }

    public String getHypnosName() {
        return hypnosName;
    }

    public HypnosJobAction setHypnosName(String hypnosName) {
        this.hypnosName = hypnosName;
        return this;
    }

    public String getActionCron() {
        return actionCron;
    }

    public HypnosJobAction setActionCron(String actionCron) {
        this.actionCron = actionCron;
        return this;
    }

    public String getDefinedCron() {
        return definedCron;
    }

    public HypnosJobAction setDefinedCron(String definedCron) {
        this.definedCron = definedCron;
        return this;
    }

    public String getResourceTypeString() {
        return resourceTypeString;
    }

    public HypnosJobAction setResourceTypeString(String resourceTypeString) {
        this.resourceTypeString = resourceTypeString;
        return this;
    }
}
