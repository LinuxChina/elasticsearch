/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
import org.elasticsearch.xpack.prelert.job.Job;

import java.io.IOException;

public class RestCloseJobAction extends BaseRestHandler {

    private final CloseJobAction.TransportAction closeJobAction;

    @Inject
    public RestCloseJobAction(Settings settings, RestController controller, CloseJobAction.TransportAction closeJobAction) {
        super(settings);
        this.closeJobAction = closeJobAction;
        controller.registerHandler(RestRequest.Method.POST, PrelertPlugin.BASE_PATH
                + "anomaly_detectors/{" + Job.ID.getPreferredName() + "}/_close", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        CloseJobAction.Request request = new CloseJobAction.Request(restRequest.param(Job.ID.getPreferredName()));
        if (restRequest.hasParam("close_timeout")) {
            request.setCloseTimeout(TimeValue.parseTimeValue(restRequest.param("close_timeout"), "close_timeout"));
        }
        return channel -> closeJobAction.execute(request, new AcknowledgedRestListener<>(channel));
    }
}
