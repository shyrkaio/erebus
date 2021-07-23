package io.github.shyrkaio.hypnos;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Singleton
public class KubernetesClientProducer {

    @Produces
    public KubernetesClient kubernetesClient() {
        // here you would create a custom client
        return new DefaultKubernetesClient();
    }

    @Named("Namespace")
    public String findMyCurrentNamespace() {
        String currentNS = System.getProperty("io.shyrka.hypnos.ns", "");
        if (!currentNS.isBlank()) {
            return currentNS;
        }

        File serviceaccountNamespace = new File("/var/run/secrets/kubernetes.io/serviceaccount/namespace");
        if (serviceaccountNamespace.exists() && serviceaccountNamespace.isFile()) {
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