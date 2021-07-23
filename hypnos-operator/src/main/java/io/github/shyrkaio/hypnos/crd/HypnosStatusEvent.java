package io.github.shyrkaio.hypnos.crd;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.StringJoiner;

@JsonDeserialize
@RegisterForReflection
public class HypnosStatusEvent {

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Integer getReplicas() {
        return replicas;
    }

    public void setReplicas(Integer replicas) {
        this.replicas = replicas;
    }



    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public HypnosStatusEvent(String namespace, String action, String name, String resourceType, Integer replicas) {
        this.namespace = namespace;
        this.action = action;
        this.name = name;
        this.resourceType = resourceType;
        this.replicas = replicas;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ",  "{", "}")
                .add("event:'"+HypnosStatusEvent.class.getSimpleName()+"'")
                .add("namespace='" + namespace + "'")
                .add("action='" + action + "'")
                .add("name='" + name + "'")
                .add("resourceType='" + resourceType + "'")
                .add("replicas=" + replicas)
                .toString();
    }

    String namespace;
    String action;
    String name;
    String resourceType;
    Integer replicas;
}
