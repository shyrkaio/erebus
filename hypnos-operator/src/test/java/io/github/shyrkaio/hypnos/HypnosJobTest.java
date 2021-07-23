package io.github.shyrkaio.hypnos;

import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.github.shyrkaio.hypnos.crd.HypnosJobAction;
import io.github.shyrkaio.hypnos.crd.HypnosStatusEvent;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.MockServer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTestResource(CustomKubernetesMockServerTestResource.class)
@QuarkusTest
public class HypnosJobTest {
    private static Logger _log = Logger.getLogger(HypnosJobTest.class.getCanonicalName());

    @MockServer
    KubernetesMockServer mockServer;

    @Test
    public void testHypnosJobAction() {
        String namespaceTargetedLabel = CustomKubernetesMockServerTestResource.LABEL_TAG_NS_FOR_NS + "="
                + CustomKubernetesMockServerTestResource.LABEL_TAG_VALUE_FOR_NS;
        String targetedLabel =CustomKubernetesMockServerTestResource.LABEL_TAG_NS_FOR_TARGET + "="
                + CustomKubernetesMockServerTestResource.LABEL_TAG_VALUE_FOR_TARGET;
        String hypnosName = "mock-junit";
        String actionCron = HypnosJobAction.ACTION_CRON_WAKEUP;
        String definedCron = "0/2 * * * *";

        HypnosJobAction action = new HypnosJobAction()
                .setNamespaceTargetedLabel(namespaceTargetedLabel)
                .setTargetedLabel(targetedLabel)
                .setHypnosName(hypnosName)
                .setActionCron(actionCron)
                .setDefinedCron(definedCron);
        _log.warning(""+action);

        List<HypnosStatusEvent> eventsLst = action.cronActionTargetedDeployments(namespaceTargetedLabel, targetedLabel, hypnosName,HypnosJobAction.ACTION_CRON_SLEEP);

        assertEquals(1, eventsLst.size());
    }


}
