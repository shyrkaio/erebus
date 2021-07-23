package io.github.shyrkaio.hypnos.crd;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HypnosDeploymentConfigJobAction {
    private static Logger _log = Logger.getLogger(HypnosDeploymentConfigJobAction.class.getCanonicalName());


    //@Todo move this into enum
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

    OpenShiftClient client;

    CustomResourceDefinitionContext crdDefinitionContext = new CustomResourceDefinitionContext.Builder()
            .withVersion("v1alpha3")
            .withScope("Cluster")
            .withGroup("shyrkaio.github.io")
            .withPlural("hypnox")
            .build();


    private String resourceTypeString = "";
    public String getMsg(String jobKey) {
        return "HypnosJob: " + jobKey + " executing at " + new Date() + "\n" +
                "  namespaceTargetedLabel is " + getNamespaceTargetedLabel() + "\n" +
                "  targetedLabel is " + getTargetedLabel() + "\n" +
                "  resourceTypeString is " + getResourceTypeString() + "\n" +
                "  actionCron is " + getActionCron() + "\n" +
                "  definedCron is " + getDefinedCron() + "\n";
    }

    private OpenShiftClient getClient(){
        if (client == null){
            client = makeDefaultClient();
        }
        return client;
    }

    OpenShiftClient makeDefaultClient() {
        //DefaultOpenShiftClient
        String currentNS = System.getProperty("io.shyrka.hypnos.ns","");
        if (!currentNS.isBlank()){
            new DefaultOpenShiftClient().inNamespace(currentNS);
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

        return new DefaultOpenShiftClient().inNamespace("default");
    }

    private Hypnos getHypnos(String hypnosName) {
        KubernetesDeserializer.registerCustomKind("io.github.shyrkaio.operator/v1alpha3", "Hypnos",
                Hypnos.class);



        Hypnos hypnos = getClient().customResources(
                Hypnos.class,
                HypnosList.class).withName(hypnosName).get();
        _log.fine("get hypnos named "+hypnosName);
        return hypnos;
    }

    public List<HypnosStatusEvent> cronActionTargetedDeploymentConfigs(String namespaceTargetedLabel, String targetedLabel, String hypnosName,
                                                                 String action) {
        List<HypnosStatusEvent> eventsLst = new ArrayList<>();
        NamespaceList myNs = getNamespaceList(namespaceTargetedLabel);
        if (myNs == null) {
            _log.warning("no Namespace with label " + namespaceTargetedLabel);
            return eventsLst;
        }
        for (Namespace ns : myNs.getItems()) {
            List<DeploymentConfig> deploys = getDeploymentConfigList(ns, targetedLabel);
            for (DeploymentConfig dep : deploys) {
                HypnosStatusEvent event = cronActionDeploymentConfig(ns, dep, hypnosName, action);
                eventsLst.add(event);
            }
        }
        return eventsLst;
    }

    private HypnosStatusEvent cronActionDeploymentConfig(Namespace ns, DeploymentConfig dep, String hypnosName, String actionCron) {

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

        getClient().deploymentConfigs().inNamespace(ns.getMetadata().getName()).createOrReplace(dep);

        _log.info("HypnosJob { DeploymentConfig : "+depName+" "+actionCron+" at "+sDate+"}");
        if(_log.isLoggable(Level.FINE)) {
            if(hypnosStatusEvent == null){
                _log.fine("hypnosStatusEvent is null");
            }
        }
        return hypnosStatusEvent;
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

    private List<DeploymentConfig> getDeploymentConfigList(Namespace ns, String targetedLabel) {
        String nsName = ns.getMetadata().getName();
        List<DeploymentConfig> deploys;
        if("".equals(targetedLabel)){
            _log.warning("targetedLabel is undefined, so select all Deployment");
            deploys = getClient().deploymentConfigs().inNamespace(nsName).list().getItems();
        }else{
            deploys = getClient().deploymentConfigs().inNamespace(nsName).withLabel(targetedLabel).list().getItems();
        }
        if(_log.isLoggable(Level.FINE)) {
            _log.fine("HypnosJob:  { name: "+this.getHypnosName()+", nbDeploymentConfig :" + deploys.size() + " with " + targetedLabel + " in "+nsName+"}");
        }
        return deploys;
    }

    public String getNamespaceTargetedLabel() {
        return namespaceTargetedLabel;
    }

    public HypnosDeploymentConfigJobAction setNamespaceTargetedLabel(String namespaceTargetedLabel) {
        this.namespaceTargetedLabel = namespaceTargetedLabel;
        return this;
    }

    public String getTargetedLabel() {
        return targetedLabel;
    }

    public HypnosDeploymentConfigJobAction setTargetedLabel(String targetedLabel) {
        this.targetedLabel = targetedLabel;
        return this;
    }

    public String getHypnosName() {
        return hypnosName;
    }

    public HypnosDeploymentConfigJobAction setHypnosName(String hypnosName) {
        this.hypnosName = hypnosName;
        return this;
    }

    public String getActionCron() {
        return actionCron;
    }

    public HypnosDeploymentConfigJobAction setActionCron(String actionCron) {
        this.actionCron = actionCron;
        return this;
    }

    public String getDefinedCron() {
        return definedCron;
    }

    public HypnosDeploymentConfigJobAction setDefinedCron(String definedCron) {
        this.definedCron = definedCron;
        return this;
    }

    public String getResourceTypeString() {
        return resourceTypeString;
    }

    public HypnosDeploymentConfigJobAction setResourceTypeString(String resourceTypeString) {
        this.resourceTypeString = resourceTypeString;
        return this;
    }
}
