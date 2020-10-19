# Basic use case deployment with correct labels

## scenario

In this scenario I have a ns `hypnos-smp-010` in witch I have a deployment `hypnos-010` of 3 nginx.

The hypnos configuration should stop every 2 min and restart it every 5 min. _It is not really perfect, yet it should be a good demonstration_

In this scenario, everything should be correctly configure. (no missing conf, no default conf) 

## Steps

oc apply -f erebus-operator/src/hypnos-scenarios/usecase-010/


