/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
import org.elasticsearch.xpack.prelert.job.persistence.JobProvider;

public class PrelertInitializationService extends AbstractComponent implements ClusterStateListener {

    private final ThreadPool threadPool;
    private final ClusterService clusterService;
    private final JobProvider jobProvider;

    public PrelertInitializationService(Settings settings, ThreadPool threadPool, ClusterService clusterService,
                                        JobProvider jobProvider) {
        super(settings);
        this.threadPool = threadPool;
        this.clusterService = clusterService;
        this.jobProvider = jobProvider;
        clusterService.add(this);
    }

    @Override
    public void clusterChanged(ClusterChangedEvent event) {
        if (event.localNodeMaster()) {
            MetaData metaData = event.state().metaData();
            if (metaData.custom(PrelertMetadata.TYPE) == null) {
                threadPool.executor(ThreadPool.Names.GENERIC).execute(() -> {
                    clusterService.submitStateUpdateTask("install-prelert-metadata", new ClusterStateUpdateTask() {
                        @Override
                        public ClusterState execute(ClusterState currentState) throws Exception {
                            ClusterState.Builder builder = new ClusterState.Builder(currentState);
                            MetaData.Builder metadataBuilder = MetaData.builder(currentState.metaData());
                            metadataBuilder.putCustom(PrelertMetadata.TYPE, PrelertMetadata.PROTO);
                            builder.metaData(metadataBuilder.build());
                            return builder.build();
                        }

                        @Override
                        public void onFailure(String source, Exception e) {
                            logger.error("unable to install prelert metadata upon startup", e);
                        }
                    });
                });
            }
            if (metaData.hasIndex(JobProvider.PRELERT_USAGE_INDEX) == false) {
                threadPool.executor(ThreadPool.Names.GENERIC).execute(() -> {
                    jobProvider.createUsageMeteringIndex((result, error) -> {
                        if (result) {
                            logger.info("successfully created prelert-usage index");
                        } else {
                            if (error instanceof ResourceAlreadyExistsException) {
                                logger.debug("not able to create prelert-usage index", error);
                            } else {
                                logger.error("not able to create prelert-usage index", error);
                            }
                        }
                    });
                });
            }
        }
    }
}
