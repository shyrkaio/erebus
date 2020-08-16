package io.github.shyrkaio.crd;

import io.fabric8.kubernetes.client.*;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.fabric8.kubernetes.api.builder.Function;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize
@RegisterForReflection
public class HypnosDoneable extends CustomResourceDoneable<Hypnos> {

public HypnosDoneable(Hypnos resource, Function<Hypnos, Hypnos> function) {
        super(resource, function);
        }
}
