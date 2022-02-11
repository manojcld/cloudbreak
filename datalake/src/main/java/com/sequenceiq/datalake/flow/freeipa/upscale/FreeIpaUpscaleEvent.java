package com.sequenceiq.datalake.flow.freeipa.upscale;

import com.sequenceiq.flow.core.FlowEvent;

public enum FreeIpaUpscaleEvent implements FlowEvent {
    FREE_IPA_UPSCALE_START_EVENT("FREEIPAUPSCALESTARTEVENT"),
    FREE_IPA_UPSCALE_IN_PROGRESS_EVENT,
    FREE_IPA_UPSCALE_SUCCESS_EVENT("FREEIPAUPSCALESUCCESSEVENT"),
    FREE_IPA_UPSCALE_FAILED_EVENT("FREEIPAUPSCALEFAILEDEVENT"),
    FREE_IPA_UPSCALE_SKIPPED_EVENT,
    FREE_IPA_UPSCALE_FAILED_HANDLED_EVENT,
    FREE_IPA_UPSCALE_FINALIZED_EVENT;

    private final String event;

    FreeIpaUpscaleEvent(String event) {
        this.event = event;
    }

    FreeIpaUpscaleEvent() {
        this.event = name();
    }

    @Override
    public String event() {
        return event;
    }
}
