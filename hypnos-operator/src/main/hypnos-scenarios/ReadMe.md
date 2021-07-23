# This Folder hold some use case for Erebus Kubernetes Operator 

In Each folder `scenario-xxx.md` describe the scenario that can be tested.
Theses are the tested scenarios.

This is both for end user information and base for the soon to be JUnit testes 

- [Nothing happen](usecase-000/) `kubectl apply -f erebus-operator/src/hypnos-scenarios/usecase-000/`
- [Nothing happen](usecase-001/) `kubectl apply -f erebus-operator/src/hypnos-scenarios/usecase-001/`
  

- [Start/stop 1 Deployments correctly labelled in a namespace correctly labelled  every 5 and 2 minutes](usecase-010/) `kubectl apply -f erebus-operator/src/hypnos-scenarios/usecase-010/`
- [Start/stop several Deployments correctly labelled in a namespace correctly labelled  every 5 and 2 minutes](usecase-012/) `kubectl apply -f erebus-operator/src/hypnos-scenarios/usecase-012/`
- [Start/stop several Deployments correctly labelled in several namespaces correctly labelled  every 5 and 2 minutes](usecase-014/) `kubectl apply -f erebus-operator/src/hypnos-scenarios/usecase-014/`


- [Start/stop 1 Deployments correctly labelled in a namespace correctly labelled  every 5 and 2 minutes, with a hpa](usecase-020/) `kubectl apply -f erebus-operator/src/hypnos-scenarios/usecase-020/`


- [Start/stop 1 StatefullSet correctly labelled in a namespace correctly labelled every 5 and 2 minutes](usecase-030/) `kubectl apply -f erebus-operator/src/hypnos-scenarios/usecase-030/`


- [Start/stop several Deployments and StatefullSet correctly labelled in a namespace correctly labelled every 5 and 2 minutes](usecase-044/) `kubectl apply -f erebus-operator/src/hypnos-scenarios/usecase-044/`

- [Start a Deployments correctly labelled in a namespace correctly labelled every monday to friday at 06:00 and stop it at 19:00 minutes](usecase-050/) `kubectl apply -f erebus-operator/src/hypnos-scenarios/usecase-050/`


- [Start/stop 1 DeploymentConfig _in OpenShift_ correctly labelled in a namespace correctly labelled every 5 and 2 minutes](usecase-080/)`kubectl apply -f erebus-operator/src/hypnos-scenarios/usecase-080/`
- [Start/stop 1 DeploymentConfig, Deployments and StatefullSet _in OpenShift_ correctly labelled in a namespace correctly labelled every 5 and 2 minutes](usecase-082/)`kubectl apply -f erebus-operator/src/hypnos-scenarios/usecase-082/`

For quartz cron syntax https://www.freeformatter.com/cron-expression-generator-quartz.html (Remember week start on Sunday)

you can look at shyrka-erebus-operators logs with :

```bash
kubectl -n shyrka-erebus-operators logs -l app=erebus-operator -f
```

you can supervise hypnos effect (on those examples) with this command :

```bash
watch -n 2 kubectl get deploy,sts -A -l io.shyrka.erebus/hypnos=sample
```

