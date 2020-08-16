package io.github.shyrkaio;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import io.github.shyrkaio.crd.Hypnos;
import io.github.shyrkaio.crd.HypnosDoneable;
import io.github.shyrkaio.crd.HypnosList;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ClientProducer {
    @Produces
    @Singleton
    MixedOperation<Hypnos, HypnosList, HypnosDoneable, Resource<Hypnos, HypnosDoneable>>

    makeCustomResourceClient(KubernetesClient defaultClient) {
        KubernetesDeserializer.registerCustomKind("io.github.shyrkaio.operator/v1alpha1", "Hypnos",
                Hypnos.class);

        CustomResourceDefinitionContext crdDefinitionContext = new CustomResourceDefinitionContext.Builder()
                .withVersion("v1alpha1")
                .withScope("Cluster")
                .withGroup("demo.k8s.io")
                .withPlural("podsets")
                .build();

        MixedOperation<Hypnos, HypnosList, HypnosDoneable, Resource<Hypnos, HypnosDoneable>> hypnosHypnosListHypnosDoneableResourceMixedOperation =
                defaultClient.customResources(crdDefinitionContext,
                                               Hypnos.class,
                                               HypnosList.class,
                                               HypnosDoneable.class);
        return hypnosHypnosListHypnosDoneableResourceMixedOperation;
    }

    @Produces
    @Singleton
    KubernetesClient makeDefaultClient(@Named("Namespace") String namespace) {
        return new DefaultKubernetesClient().inNamespace(namespace);
    }

    @Produces
    @Singleton
    @Named("Namespace")
    public String findMyCurrentNamespace() {
        String currentNS = System.getProperty("io.shyrka.hypnos.ns","");
        if (!currentNS.isBlank()){
            return currentNS;
        }

        File serviceaccountNamespace = new File("/var/run/secrets/kubernetes.io/serviceaccount/namespace");
        if (serviceaccountNamespace.exists() && serviceaccountNamespace.isFile()){
        try {
            currentNS = new String(Files.readAllBytes(Paths.get(serviceaccountNamespace.getPath())));
            return currentNS;
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
        }
        System.err.println("no value define for current Namespace");
        return "";
    }
}
