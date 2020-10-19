[![Shyrka](https://www.parisjug.org/xwiki/wiki/oldversion/download/Dark/Shryka/shyrka-64-erebus.svg)](https://quarkus.io/)

![AppVeyor](https://img.shields.io/appveyor/build/kanedafromparis/erebus?style=flat-square)
[![License](https://img.shields.io/github/license/quarkusio/quarkus?style=for-the-badge&logo=apache)](https://www.apache.org/licenses/LICENSE-2.0)
[![Supported JVM Versions](https://img.shields.io/badge/JVM-11-brightgreen.svg?style=for-the-badge&logo=Java)](https://github.com/quarkusio/quarkus/actions/runs/113853915/)


# Shyrka : erebus project

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

Erebus is one of the kubernetes operator of the shyrka project. 

If you want to know more about the [shyrka project](https://shyrka.io).

This is a personal wannabe community project.
- Hypnos - cron scale down and up for Deployment, StatefulSet [<a href="https://en.wikipedia.org/wiki/Hypnos">about the name</a>]</small>
- CaiusPupus - conformity check (Not implemented yet)<small>[<a href="https://en.wikipedia.org/wiki/Caius_Pupus">about the name</a>]</small>
- Lethe - Project ownership reminder (Not implemented yet)<small>[<a href="https://en.wikipedia.org/wiki/Lethe">about the name</a>]</small>
- Moros - backup (Not implemented yet) <small>[<a href="https://en.wikipedia.org/wiki/Moros">about the name</a>]</small>


## Hypnos

Hypnos will scale down the resources at a certain frequences (define by sleep-cron) and scale it up at a certain frequences (define by wakeup-cron)
  
  You can create Hypnos strategy like this :
  
```yaml
apiVersion: shyrkaio.github.io/v1alpha1
kind: Hypnos
metadata:
  name: test-011
  #...
spec:
  namespaceTargetedLabel: "io.shyrka.erebus/hypnos=sample010"
  resourceType:
    - "Deployment"
    - "StatefulSet" #not-implemented-yet
  targetedLabel: "io.shyrka.erebus/hypnos=sample"
  # this is for testing regular cron should be
  wakeup-cron: "0/5 * * * *"
  sleep-cron:  "0/2 * * * *"

  #not-implemented-yet
  load-policy: no-action #not-implemented-yet
  dry-run: false #not-implemented-yet
  pause: false #not-implemented-yet
  comments: "just a simple comments"
```

  - <i>namespaceTargetedLabel</i> : define the label used for the targeted namespaces 
  - <i>resourceType</i> : define the resources that will be scale down and up within the targeted namespaces
  - <i>targetedLabel</i> : define the label used for the targeted resources within the targeted namespaces
  - <i>sleep-cron</i> : define the cron frequence of the scale-down
  - <i>wakeup-cron</i> : define the cron frequence of the scale-up
  - <i>load-policy</i> : define the action made after an update of the Hypnos strategy (run-sleep-on-change, run-wake-up-on-change, no-action)
  - <i>dry-run</i> : only update the status to see the targeted resources
  - <i>pause</i> : stop the hypnos strategy to be apply
  - <i>comments</i> : is for inner comments


## Installation

### from build

Retrieve the 'develop' branch 
```bash
git clone https://github.com/shyrkaio/erebus && \
cd erebus/ && git checkout develop
```

Set the target registry
```bash
REG_HOST=quay.io
REG_IMG_HOST=$REG_HOST/shyrkaio/erebus-operator
```

Set the target registry into maven configuration for [more details](http://maven.apache.org/settings.html#) 
```bash
cat <<'EOF' > settings-sample.xml
<settings>
<servers>
  <server>
    <id>quay.io</id>
    <username>XXXXXXXXXXX</username>
    <password>...........</password>
  </server>
</servers>
</settings>
EOF
clear
```

Create the CRD (Custom Resource Definition)

```bash
kubectl apply -f erebus-operator/src/main/crd/
```

Build the java application

```bash
mvn package -pl erebus-operator -Dquarkus.container-image.registry=$REG_HOST 
# test are in progress so you might need
#-Dmaven.test.skip=true
```

Build the container locally and push it to your registry
```bash
mvn k8s:build k8s:push -Pkubernetes \ 
          -pl erebus-operator \
          -Djkube.build.strategy=jib \ 
          -Djkube.docker.registry=$REG_HOST \
          -Djkube.image.name=$REG_IMG_HOST:latest \
          -Djkube.docker.push.registry=$REG_HOST \
          -Dquarkus.container-image.registry=$REG_HOST \ 
          -Dquarkus.container-image.build=true  
          #-Dmaven.test.skip=true
```

Create the kubernetes resources definition and apply them to your cluster (you are supposed to be logged)

```bash
mvn k8s:resource k8s:apply -Pkubernetes \
             -pl erebus-operator \
             -Djkube.build.strategy=jib \
             -Djkube.docker.registry=$REG_HOST \
             -Djkube.image.name=$REG_IMG_HOST:latest \
             -Djkube.docker.push.registry=$REG_HOST \
             -Dquarkus.container-image.registry=$REG_HOST \
             -Dquarkus.container-image.build=true 
#-Dmaven.test.skip=true
```

you can see operator logs with `kubectl logs -l app=erebus-operator -n shyrka-erebus-operators -f`

### from helm 
@Todo

### from operator hub
@Todo