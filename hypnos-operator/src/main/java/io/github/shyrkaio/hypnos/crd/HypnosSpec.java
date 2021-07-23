package io.github.shyrkaio.hypnos.crd;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@RegisterForReflection
public class HypnosSpec {

    public String getNamespaceTargetedLabel() {
        return namespaceTargetedLabel;
    }

    public void setNamespaceTargetedLabel(String namespaceTargetedLabel) {
        this.namespaceTargetedLabel = namespaceTargetedLabel;
    }

    public String getTargetedLabel() {
        return targetedLabel;
    }

    public void setTargetedLabel(String targetedLabel) {
        this.targetedLabel = targetedLabel;
    }

    public String[] getResourceType() {
        return resourceType;
    }

    public void setResourceType(String[] resourceType) {
        this.resourceType = resourceType;
    }

    public String getWakeupCron() {
        return wakeupCron;
    }

    public void setWakeupCron(String wakeupCron) {
        this.wakeupCron = wakeupCron.trim();
    }

    public String getSleepCron() {
        return sleepCron;
    }

    public void setSleepCron(String sleepCron) {
        this.sleepCron = sleepCron.trim();
    }

    public String getCronType() {
        return cronType;
    }

    public void setCronType(String cronType) {
        this.cronType = cronType;
    }

    public String getLoadPolicy() {
        return loadPolicy;
    }

    public void setLoadPolicy(String loadPolicy) {
        this.loadPolicy = loadPolicy;
    }

    public Boolean getDryRun() {
        return dryRun;
    }

    public void setDryRun(Boolean dryRun) {
        this.dryRun = dryRun;
    }

    public Boolean getPause() {
        return pause;
    }

    public void setPause(Boolean pause) {
        this.pause = pause;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    @JsonProperty("namespaceTargetedLabel")
    private String namespaceTargetedLabel;

    @JsonProperty("targetedLabel")
    private String targetedLabel;

    @JsonProperty("resourceType")
    private String[] resourceType;

    @JsonProperty("cron-type")
    private String cronType;

    @JsonProperty("wakeup-cron")
    private String wakeupCron;

    @JsonProperty("sleep-cron")
    private String sleepCron;

    @JsonProperty("load-policy")
    private String loadPolicy;

    @JsonProperty("dry-run")
    private Boolean dryRun;

    @JsonProperty("pause")
    private Boolean pause;

    @JsonProperty("comments")
    private String comments;


    @Override
    public String toString(){
        return "{'namespaceTargetedLabel' : "+namespaceTargetedLabel+
                ",'targetedLabel' : "+targetedLabel+
                ",'resourceType' : ["+String.join(",", resourceType)+"]"+
                ",'cron-type' : "+ getCronType() +
                ",'wakeup-cron' : "+wakeupCron+
                ",'sleep-cron' : "+sleepCron+
                ",'load-policy' : "+ getLoadPolicy() +
                ",'dry-run' : "+ getDryRun() +
                ",'pause' : "+ getPause() +
                ",'comments' : "+ getComments() +
                "}";
    }

}
