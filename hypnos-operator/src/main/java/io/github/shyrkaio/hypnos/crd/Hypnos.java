package io.github.shyrkaio.hypnos.crd;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize
@RegisterForReflection
@Group("shyrkaio.github.io")
@Version("v1alpha3")
@Plural("hypnox")
public class Hypnos extends CustomResource {
    /**
     *
     */
    private static final long serialVersionUID = -2087225423801011841L;

    private HypnosSpec spec;
    private HypnosStatus status;

    public HypnosSpec getSpec() {
        return spec;
    }

    public void setSpec(HypnosSpec spec) {
        this.spec = spec;

    }

    public HypnosStatus getStatus() {
        return status;
    }

    public void setStatus(HypnosStatus status) {
        this.status = status;

    }
    @Override
    public String getApiVersion() {
        return "shyrkaio.github.io/v1alpha1";
    }

    @Override
    public String toString(){
        return "{name:"+this.getMetadata().getName()+", version:"+this.getMetadata().getResourceVersion()+", spec:"+this.getSpec()+"}";
    }
}
