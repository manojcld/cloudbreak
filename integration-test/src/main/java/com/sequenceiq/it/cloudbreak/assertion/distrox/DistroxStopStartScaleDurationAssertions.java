package com.sequenceiq.it.cloudbreak.assertion.distrox;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventContainer;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.legacy.OperationDetails;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class DistroxStopStartScaleDurationAssertions implements Assertion<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroxStopStartScaleDurationAssertions.class);

    private final long expectedDuration;

    private final boolean scalingUp;

    public DistroxStopStartScaleDurationAssertions(long expectedDuration, boolean scalingUp) {
        this.expectedDuration = expectedDuration;
        this.scalingUp = scalingUp;
    }

    @Override
    public DistroXTestDto doAssertion(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        String startMessage = scalingUp ? "Scaling up (via instance start) for host group" : "Scaling down (via instance stop) for host group";
        String stopMessage = scalingUp ? "Scaled up (via instance start) host group" : "Scaled down (via instance stop) host group";

        StructuredEventContainer structuredEventContainer = client.getDefaultClient()
                .eventV4Endpoint()
                .structured(testDto.getName(), testContext.getActingUserCrn().getAccountId());
        List<StructuredNotificationEvent> structuredNotificationEvents = structuredEventContainer.getNotification();

        long startTime = structuredNotificationEvents.stream()
                .filter(events -> events.getNotificationDetails().getNotification()
                        .equalsIgnoreCase(startMessage))
                .map(StructuredEvent::getOperation).max(Comparator.comparing(OperationDetails::getTimestamp)).get().getTimestamp();
        long endTime = structuredNotificationEvents.stream()
                .filter(events -> events.getNotificationDetails().getNotification()
                        .equalsIgnoreCase(stopMessage))
                .map(StructuredEvent::getOperation).max(Comparator.comparing(OperationDetails::getTimestamp)).get().getTimestamp();
        long actualDuration = endTime - startTime;

        LOGGER.info(String.format("Start time: %s for %s", new Date(startTime * 1000), startMessage));
        LOGGER.info(String.format("End time: %s for %s", new Date(endTime * 1000), startMessage));

        String message = String.format("Distrox last scale have been took (%d) more than the expected %d minutes!",
                TimeUnit.MILLISECONDS.toMinutes(actualDuration), expectedDuration);
        if (actualDuration > TimeUnit.MINUTES.toMillis(expectedDuration)) {
            LOGGER.error(message);
            throw new TestFailException(message);
        }
        return testDto;
    }
}
