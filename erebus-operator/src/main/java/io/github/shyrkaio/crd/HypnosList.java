package io.github.shyrkaio.crd;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@RegisterForReflection
public class HypnosList extends CustomResourceList<Hypnos> {

    /**
     *
     */
    private static final long serialVersionUID = 1696633306337049795L;

}
