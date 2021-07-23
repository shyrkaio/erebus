package io.github.shyrkaio.hypnos.crd;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.ListMeta;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.ArrayList;
import java.util.List;

@JsonDeserialize
@RegisterForReflection
public class HypnosList implements KubernetesResourceList<Hypnos> {

    /**
     *
     */
    private static final long serialVersionUID = 1696633306337049795L;

    private List<Hypnos> hypnos = new ArrayList<Hypnos>();

    private ListMeta meta = new ListMeta();
    @Override
    public ListMeta getMetadata() {
        return meta;
    }

    @Override
    public List<Hypnos> getItems() {
        return hypnos;
    }
}
