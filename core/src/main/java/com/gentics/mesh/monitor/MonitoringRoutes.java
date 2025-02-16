package com.gentics.mesh.monitor;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;
import static io.vertx.core.http.HttpMethod.GET;

import javax.inject.Inject;

import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.endpoint.admin.AdminHandler;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.handler.VersionHandler;
import com.gentics.mesh.router.route.DefaultNotFoundHandler;
import com.gentics.mesh.router.route.FailureHandler;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.impl.RouterImpl;
import io.vertx.micrometer.PrometheusScrapingHandler;

public class MonitoringRoutes {

	private static final Logger log = LoggerFactory.getLogger(MonitoringRoutes.class);

	private final BootstrapInitializer boot;

	private final RouterImpl router;

	private final RouterImpl apiRouter;

	private final AdminHandler adminHandler;

	private final MeshOptions options;

	@Inject
	public MonitoringRoutes(Vertx vertx, BootstrapInitializer boot, AdminHandler adminHandler, MeshOptions options) {
		this.router = new RouterImpl(vertx);
		this.boot = boot;
		this.apiRouter = new RouterImpl(vertx);
		this.options = options;
		VersionHandler.generateVersionMountpoints()
			.forEach(mountPoint -> router.mountSubRouter(mountPoint, apiRouter));
		this.adminHandler = adminHandler;
		init();
	}

	public void init() {
		router.route().handler(LoggerHandler.create());
		router.route().last().handler(DefaultNotFoundHandler.create());
		router.route().failureHandler(FailureHandler.create());

		addMetrics();
		addLive();
		addReady();
		addVersion();
		addStatus();
		addClusterStatus();
	}

	/**
	 * Handler that reacts onto status requests.
	 */
	private void addStatus() {
		// endpoint.description("Return the Gentics Mesh server status.");
		// endpoint.exampleResponse(OK, adminExamples.createMeshStatusResponse(MeshStatus.READY), "Status of the Gentics Mesh server.");
		apiRouter.route("/status")
			.method(GET)
			.produces(APPLICATION_JSON)
			.handler(rc -> {
				InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
				adminHandler.handleMeshStatus(ac);
			});
	}

	private void addClusterStatus() {
		// endpoint.description("Loads the cluster status information.");
		// endpoint.exampleResponse(OK, adminExamples.createClusterStatusResponse(), "Cluster status.");
		apiRouter.route("/cluster/status")
			.method(GET)
			.produces(APPLICATION_JSON)
			.handler(rc -> {
				InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
				adminHandler.handleClusterStatus(ac);
			});
	}

	private void addLive() {
		apiRouter.route("/health/live")
			.method(GET)
			.handler(rc -> {
				// We currently don't have a situation which would justify to let the service being restarted automatically.
				rc.response().setStatusCode(200).end();
			});
	}

	private void addReady() {
		apiRouter.route("/health/ready")
			.method(GET)
			.handler(rc -> {
				MeshStatus status = boot.mesh().getStatus();
				if (status.equals(MeshStatus.READY)) {
					rc.response().end();
				} else {
					if (log.isDebugEnabled()) {
						log.debug("Status is {" + status.name() + "} - Failing readiness probe");
					}
					throw error(SERVICE_UNAVAILABLE, "error_internal");
				}
			});
	}

	private void addMetrics() {
		// metrics.description("Returns the stored system metrics.");
		apiRouter.route("/metrics")
			.handler(PrometheusScrapingHandler.create(options.getNodeName()));
	}

	private void addVersion() {
		// infoEndpoint.description("Endpoint which returns version information");
		// infoEndpoint.displayName("Version Information");
		// infoEndpoint.exampleResponse(OK, examples.getInfoExample(), "JSON which contains version information");
		apiRouter.route("/versions")
			.produces(APPLICATION_JSON)
			.method(GET)
			.handler(rc -> {
				InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
				adminHandler.handleVersions(ac);
			});
	}

	public Router getRouter() {
		return router;
	}
}
