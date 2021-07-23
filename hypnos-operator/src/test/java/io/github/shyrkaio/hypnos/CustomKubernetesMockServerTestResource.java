package io.github.shyrkaio.hypnos;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.github.shyrkaio.hypnos.crd.Hypnos;
import io.github.shyrkaio.hypnos.crd.HypnosList;
import io.github.shyrkaio.hypnos.crd.HypnosSpec;
import io.quarkus.test.kubernetes.client.KubernetesMockServerTestResource;

import java.util.Map;

public class CustomKubernetesMockServerTestResource extends KubernetesMockServerTestResource {

    public static String LABEL_TAG_NS_FOR_NS = "io.shyrka.erebus/hypnos";
    public static String LABEL_TAG_VALUE_FOR_NS = "junit-sample010";
    public static String LABEL_TAG_NS_FOR_TARGET = "io.shyrka.erebus/hypnos";
    public static String LABEL_TAG_VALUE_FOR_TARGET = "junit-sample";
    public static String LABEL_TAG_NS_FOR_HYPNOS = "io.shyrka.erebus/role";
    public static String LABEL_TAG_VALUE_FOR_HYPNOS = "junit-test";

    Map<String, String> nsLabels = Map.of(CustomKubernetesMockServerTestResource.LABEL_TAG_NS_FOR_NS,
            CustomKubernetesMockServerTestResource.LABEL_TAG_VALUE_FOR_NS);
    Map<String, String> deployLabels = Map.of(CustomKubernetesMockServerTestResource.LABEL_TAG_NS_FOR_TARGET,
            CustomKubernetesMockServerTestResource.LABEL_TAG_VALUE_FOR_TARGET);

    DeploymentList deployLst;
    NamespaceList nsLst;
    HypnosList hypnoslst;

    @Override
    public void configureMockServer(KubernetesMockServer mockServer) {

        mockServer.expect().get().withPath("/apis/shyrkaio.github.io/v1alpha1/hypnox?watch=true")
                .andReturn(200, this.getHypnos001())
                .always();

        mockServer.expect().get().withPath("/api/v1/namespaces?labelSelector=io.shyrka.erebus%2Fhypnos%3Djunit-sample010")
                .andReturn(200, this.getNamespaceList())
                .always();

        mockServer.expect().get().withPath("/apis/apps/v1/namespaces/hypnos-junit-010/deployments?labelSelector=io.shyrka.erebus%2Fhypnos%3Djunit-sample")
                .andReturn(200, this.getDeploymentList()).times(3);

        mockServer.expect().get().withPath("/apis/apps/v1/namespaces/hypnos-junit-010/deployments/hypnos-010")
                .andReturn(200, this.getDeploymentList()).times(3);
        
        mockServer.expect().get().withPath("/apis/extensions/v1beta1/namespaces/hypnos-junit-010/deployments/hypnos-010")
                .andReturn(550, new DeploymentList()).always();

        mockServer.expect().post().withPath("/apis/apps/v1/namespaces/hypnos-junit-010/deployments")
                .andReturn(200, this.getDeploymentList()).times(3);

        mockServer.expect().put().withPath("/apis/apps/v1/namespaces/hypnos-junit-010/deployments/hypnos-010")
                .andReturn(200, this.getDeploymentList()).times(3);

        mockServer.expect().put().withPath("/apis/extensions/v1beta1/namespaces/hypnos-junit-010/deployments/hypnos-010")
                .andReturn(550, new DeploymentList()).always();

    }

    private HypnosList getHypnos001() {
        if(this.hypnoslst != null){
            return  this.hypnoslst;
        }
        this.hypnoslst = new HypnosList();
        Hypnos hypnos = new Hypnos();

        hypnos.getMetadata().setName("junit-hypnos-010");
        Map<String, String> hypNosLabels = Map.of(LABEL_TAG_NS_FOR_TARGET, LABEL_TAG_VALUE_FOR_TARGET,
                LABEL_TAG_NS_FOR_HYPNOS, LABEL_TAG_VALUE_FOR_HYPNOS);
        hypnos.getMetadata().setLabels(hypNosLabels);
        HypnosSpec hSpec = new HypnosSpec();
        hSpec.setNamespaceTargetedLabel(LABEL_TAG_NS_FOR_NS + "=" + LABEL_TAG_VALUE_FOR_NS);
        hSpec.setTargetedLabel(LABEL_TAG_NS_FOR_TARGET + "=" + LABEL_TAG_VALUE_FOR_TARGET);
        String[] resType = new String[]{"Deployment"};
        hSpec.setResourceType(resType);
        hSpec.setWakeupCron("0/5 * * * *");
        hSpec.setSleepCron("0/2 * * * *");

        hSpec.setLoadPolicy("no-action");
        hSpec.setDryRun(false);
        hSpec.setPause(false);
        hSpec.setComments("just no comments");

        hypnos.setSpec(hSpec);

        this.hypnoslst.getItems().add(hypnos);
        return this.hypnoslst;
    }

    private NamespaceList getNamespaceList(){
        if(this.nsLst != null){
            return  this.nsLst;
        }
        this.nsLst = new NamespaceList();
        Namespace smp = new NamespaceBuilder().withNewMetadata().withName("hypnos-junit-010").withLabels(nsLabels).endMetadata().build();
        this.nsLst.getItems().add(smp);
        return this.nsLst;
    }

    private DeploymentList getDeploymentList(){
        if(this.deployLst != null){
            return  this.deployLst;
        }
        this.deployLst = new DeploymentList();
        Deployment dep;
        dep = new DeploymentBuilder()
                .withNewMetadata().withName("hypnos-010").withLabels(deployLabels).endMetadata()
                .withNewSpec().withReplicas(3)
                .withNewTemplate().withNewSpec()
                .withContainers().addNewContainer().withImage("quay.io/kanedafromparis/nginx:ocp-0.3").endContainer()
                .endSpec().endTemplate()
                .endSpec()
                .build();
        this.deployLst.getItems().add(dep);
        return this.deployLst;
    }
}
