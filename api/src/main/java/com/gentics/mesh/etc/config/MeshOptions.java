package com.gentics.mesh.etc.config;

import java.io.File;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;

/**
 * Main mesh configuration POJO.
 */
@GenerateDocumentation
public class MeshOptions implements Option {

	public static final String DEFAULT_LANGUAGE = "en";
	public static final String DEFAULT_DIRECTORY_NAME = "graphdb";
	public static final int DEFAULT_MAX_DEPTH = 10;
	public static final int DEFAULT_PLUGIN_TIMEOUT = 15;

	public static final String MESH_DEFAULT_LANG_ENV = "MESH_DEFAULT_LANG";
	public static final String MESH_LANGUAGES_FILE_PATH_ENV = "MESH_LANGUAGES_FILE_PATH";
	public static final String MESH_UPDATECHECK_ENV = "MESH_UPDATECHECK";
	public static final String MESH_TEMP_DIR_ENV = "MESH_TEMP_DIR";
	public static final String MESH_PLUGIN_DIR_ENV = "MESH_PLUGIN_DIR";
	public static final String MESH_PLUGIN_TIMEOUT_ENV = "MESH_PLUGIN_TIMEOUT";
	public static final String MESH_NODE_NAME_ENV = "MESH_NODE_NAME";
	public static final String MESH_CLUSTER_INIT_ENV = "MESH_CLUSTER_INIT";
	public static final String MESH_LOCK_PATH_ENV = "MESH_LOCK_PATH";

	// TODO remove this setting. There should not be a default max depth. This is no longer needed once we remove the expand all parameter
	private int defaultMaxDepth = DEFAULT_MAX_DEPTH;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure system wide default language. This language is automatically used if no language has been specified within the REST query parameters or GraphQL query arguments.")
	@EnvironmentVariable(name = MESH_DEFAULT_LANG_ENV, description = "Override the configured default language.")
	private String defaultLanguage = DEFAULT_LANGUAGE;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Optional path to a JSON file containing additional languages")
	@EnvironmentVariable(name = MESH_LANGUAGES_FILE_PATH_ENV, description = "Override the path to the optional languages file")
	private String languagesFilePath;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Turn on or off the update checker.")
	@EnvironmentVariable(name = MESH_UPDATECHECK_ENV, description = "Override the configured updatecheck flag.")
	private boolean updateCheck = true;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Http server options.")
	private HttpServerConfig httpServerOptions = new HttpServerConfig();

	@JsonProperty(required = true)
	@JsonPropertyDescription("Monitoring options.")
	private MonitoringConfig monitoringOptions = new MonitoringConfig();

	@JsonProperty(required = true)
	@JsonPropertyDescription("Vert.x specific options.")
	private VertxOptions vertxOptions = new VertxOptions();

	@JsonProperty(required = true)
	@JsonPropertyDescription("Cluster options.")
	private ClusterOptions clusterOptions = new ClusterOptions();

	@JsonProperty(required = true)
	@JsonPropertyDescription("Graph database options.")
	private GraphStorageOptions storageOptions = new GraphStorageOptions();

	@JsonProperty(required = true)
	@JsonPropertyDescription("Search engine options.")
	private ElasticSearchOptions searchOptions = new ElasticSearchOptions();

	@JsonProperty(required = true)
	@JsonPropertyDescription("File upload options.")
	private MeshUploadOptions uploadOptions = new MeshUploadOptions();

	@JsonProperty(required = true)
	@JsonPropertyDescription("Authentication options.")
	private AuthenticationOptions authenticationOptions = new AuthenticationOptions();

	@JsonProperty(required = true)
	@JsonPropertyDescription("Image handling options.")
	private ImageManipulatorOptions imageOptions = new ImageManipulatorOptions();

	@JsonProperty(required = true)
	@JsonPropertyDescription("Content related options.")
	private ContentConfig contentOptions = new ContentConfig();

	@JsonProperty(required = true)
	@JsonPropertyDescription("Cache options.")
	private CacheConfig cacheConfig = new CacheConfig();

	@JsonProperty(required = true)
	@JsonPropertyDescription("Debug info options.")
	private DebugInfoOptions debugInfoOptions = new DebugInfoOptions();

	@JsonProperty(required = false)
	@JsonPropertyDescription("Path to the central tmp directory.")
	@EnvironmentVariable(name = MESH_TEMP_DIR_ENV, description = "Override the configured temp directory.")
	private String tempDirectory = "data" + File.separator + "tmp";

	@JsonProperty(required = false)
	@JsonPropertyDescription("Path to the plugin directory.")
	@EnvironmentVariable(name = MESH_PLUGIN_DIR_ENV, description = "Override the configured plugin directory.")
	private String pluginDirectory = "plugins";

	@JsonProperty(required = false)
	@JsonPropertyDescription("Timeout in seconds which is used for the plugin startup,initialization,de-initialization and stop processes.")
	@EnvironmentVariable(name = MESH_PLUGIN_TIMEOUT_ENV, description = "Override the configured plugin timeout.")
	private int pluginTimeout = DEFAULT_PLUGIN_TIMEOUT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Name of the cluster node instance. If not specified a name will be generated.")
	@EnvironmentVariable(name = MESH_NODE_NAME_ENV, description = "Override the configured node name.")
	private String nodeName;

	/* EXTRA Command Line Arguments */
	@JsonIgnore
	@EnvironmentVariable(name = MESH_CLUSTER_INIT_ENV, description = "Enable or disable the initial cluster database setup. This is useful for testing.")
	private boolean isInitCluster = false;

	@JsonIgnore
	@EnvironmentVariable(name = MESH_LOCK_PATH_ENV, description = "Path to the mesh lock file.")
	private String lockPath = "mesh.lock";

	@JsonIgnore
	private String adminPassword;

	public MeshOptions() {
	}

	/**
	 * Return the default language.
	 * 
	 * @return Language tag of the default language
	 */
	public String getDefaultLanguage() {
		return defaultLanguage;
	}

	/**
	 * Return the (optional) languages file path
	 * 
	 * @return path to the optional languages file
	 */
	public String getLanguagesFilePath() {
		return languagesFilePath;
	}

	/**
	 * Set the languages file path
	 * 
	 * @param languagesFilePath
	 *            path to the optional languages file
	 */
	public void setLanguagesFilePath(String languagesFilePath) {
		this.languagesFilePath = languagesFilePath;
	}

	/**
	 * Return the default max depth for navigations.
	 * 
	 * @return
	 */
	public int getDefaultMaxDepth() {
		return defaultMaxDepth;
	}

	/**
	 * Set the default max depth for navigations.
	 * 
	 * @param defaultMaxDepth
	 * @return Fluent API
	 */
	public MeshOptions setDefaultMaxDepth(int defaultMaxDepth) {
		this.defaultMaxDepth = defaultMaxDepth;
		return this;
	}

	/**
	 * Return the mesh graph database storage options.
	 * 
	 * @return Storage options
	 */
	@JsonProperty("storage")
	public GraphStorageOptions getStorageOptions() {
		return this.storageOptions;
	}

	/**
	 * Return the mesh upload options.
	 * 
	 * @return Upload options
	 */
	@JsonProperty("upload")
	public MeshUploadOptions getUploadOptions() {
		return uploadOptions;
	}

	/**
	 * Set the mesh upload options.
	 * 
	 * @param uploadOptions
	 *            Upload options
	 */
	public void setUploadOptions(MeshUploadOptions uploadOptions) {
		this.uploadOptions = uploadOptions;
	}

	/**
	 * Return the http server options.
	 * 
	 * @return Http server options
	 */
	@JsonProperty("httpServer")
	public HttpServerConfig getHttpServerOptions() {
		return httpServerOptions;
	}

	/**
	 * Set the http server options.
	 * 
	 * @param httpServerOptions
	 *            Http server options
	 */
	public void setHttpServerOptions(HttpServerConfig httpServerOptions) {
		this.httpServerOptions = httpServerOptions;
	}

	@JsonProperty("monitoring")
	public MonitoringConfig getMonitoringOptions() {
		return monitoringOptions;
	}

	/**
	 * Set the monitoring options.
	 * 
	 * @param monitoringOptions
	 */
	public void setMonitoringOptions(MonitoringConfig monitoringOptions) {
		this.monitoringOptions = monitoringOptions;
	}

	public VertxOptions getVertxOptions() {
		return vertxOptions;
	}

	@JsonProperty("cluster")
	public ClusterOptions getClusterOptions() {
		return clusterOptions;
	}

	public void setClusterOptions(ClusterOptions clusterOptions) {
		this.clusterOptions = clusterOptions;
	}

	/**
	 * Return the search options.
	 * 
	 * @return Search options
	 */
	@JsonProperty("search")
	public ElasticSearchOptions getSearchOptions() {
		return searchOptions;
	}

	/**
	 * Set the search options.
	 * 
	 * @param searchOptions
	 *            Search options
	 * @return Fluent API
	 */
	public MeshOptions setSearchOptions(ElasticSearchOptions searchOptions) {
		this.searchOptions = searchOptions;
		return this;
	}

	/**
	 * Return the authentication options
	 * 
	 * @return Authentication options
	 */
	@JsonProperty("security")
	public AuthenticationOptions getAuthenticationOptions() {
		return authenticationOptions;
	}

	/**
	 * Set the authentication options
	 * 
	 * @param authenticationOptions
	 *            Authentication options
	 * @return Fluent API
	 */
	public MeshOptions setAuthenticationOptions(AuthenticationOptions authenticationOptions) {
		this.authenticationOptions = authenticationOptions;
		return this;
	}

	/**
	 * Returns the temporary directory.
	 * 
	 * @return Temporary filesystem directory
	 */
	public String getTempDirectory() {
		return tempDirectory;
	}

	/**
	 * Set the temporary directory.
	 * 
	 * @param tempDirectory
	 *            Temporary filesystem directory
	 */
	public void setTempDirectory(String tempDirectory) {
		this.tempDirectory = tempDirectory;
	}

	/**
	 * Return the plugin directory.
	 * 
	 * @return
	 */
	public String getPluginDirectory() {
		return pluginDirectory;
	}

	/**
	 * Set the plugin directory.
	 * 
	 * @param pluginDirectory
	 */
	public void setPluginDirectory(String pluginDirectory) {
		this.pluginDirectory = pluginDirectory;
	}

	/**
	 * Return the image manipulation options.
	 * 
	 * @return
	 */
	@JsonProperty("image")
	public ImageManipulatorOptions getImageOptions() {
		return imageOptions;
	}

	/**
	 * Set the image manipulation options.
	 * 
	 * @param imageOptions
	 * @return Fluent API
	 */
	public MeshOptions setImageOptions(ImageManipulatorOptions imageOptions) {
		this.imageOptions = imageOptions;
		return this;
	}

	/**
	 * Return the content options
	 * 
	 * @return
	 */
	@JsonProperty("content")
	public ContentConfig getContentOptions() {
		return contentOptions;
	}

	/**
	 * Set the content options
	 * 
	 * @param contentOptions
	 * @return
	 */
	public MeshOptions setContentOptions(ContentConfig contentOptions) {
		this.contentOptions = contentOptions;
		return this;
	}

	/**
	 * Return the debug info options
	 * @return
	 */
	@JsonProperty("debugInfo")
	public DebugInfoOptions getDebugInfoOptions() {
		return debugInfoOptions;
	}

	/**
	 * Set the debug info options
	 * @param debugInfoOptions
	 * @return
	 */
	public MeshOptions setDebugInfoOptions(DebugInfoOptions debugInfoOptions) {
		this.debugInfoOptions = debugInfoOptions;
		return this;
	}

	/**
	 * Return the cache options.
	 * 
	 * @return
	 */
	@JsonProperty("cache")
	public CacheConfig getCacheConfig() {
		return cacheConfig;
	}

	/**
	 * Set the cache options.
	 * 
	 * @param cacheConfig
	 * @return
	 */
	public MeshOptions setCacheConfig(CacheConfig cacheConfig) {
		this.cacheConfig = cacheConfig;
		return this;
	}

	/**
	 * Return update checker flag.
	 * 
	 * @return
	 */
	@JsonProperty("updateCheck")
	public boolean isUpdateCheckEnabled() {
		return updateCheck;
	}

	/**
	 * Set the update checker flag. If set to true a update check will be invoked during mesh server startup.
	 * 
	 * @param updateCheck
	 * @return Fluent API
	 */
	public MeshOptions setUpdateCheck(boolean updateCheck) {
		this.updateCheck = updateCheck;
		return this;
	}

	/**
	 * Set the node name.
	 * 
	 * @param nodeName
	 * @return
	 */
	public MeshOptions setNodeName(String nodeName) {
		this.nodeName = nodeName;
		return this;
	}

	/**
	 * Return the node name.
	 * 
	 * @return
	 */
	public String getNodeName() {
		return nodeName;
	}

	@JsonIgnore
	public boolean isInitClusterMode() {
		return isInitCluster;
	}

	@JsonIgnore
	public MeshOptions setInitCluster(boolean isInitCluster) {
		this.isInitCluster = isInitCluster;
		return this;
	}

	@JsonIgnore
	public String getLockPath() {
		return lockPath;
	}

	@JsonIgnore
	public MeshOptions setLockPath(String lockPath) {
		this.lockPath = lockPath;
		return this;
	}

	@JsonIgnore
	public String getAdminPassword() {
		return adminPassword;
	}

	@JsonIgnore
	public MeshOptions setAdminPassword(String adminPassword) {
		this.adminPassword = adminPassword;
		return this;
	}

	public int getPluginTimeout() {
		return pluginTimeout;
	}

	public MeshOptions setPluginTimeout(int pluginTimeout) {
		this.pluginTimeout = pluginTimeout;
		return this;
	}

	public void validate() {
		if (getClusterOptions() != null) {
			getClusterOptions().validate(this);
		}
		if (getStorageOptions() != null) {
			getStorageOptions().validate(this);
		}
		if (getSearchOptions() != null) {
			getSearchOptions().validate(this);
		}
		if (getHttpServerOptions() != null) {
			getHttpServerOptions().validate(this);
		}
		if (getAuthenticationOptions() != null) {
			getAuthenticationOptions().validate(this);
		}
		if (getImageOptions() != null) {
			getImageOptions().validate(this);
		}
		if (getMonitoringOptions() != null) {
			getMonitoringOptions().validate(this);
		}
		if (getContentOptions() != null) {
			getContentOptions().validate(this);
		}
		Objects.requireNonNull(getNodeName(), "The node name must be specified.");
		// TODO check for other invalid characters in node name
	}

	@Override
	public void validate(MeshOptions options) {
		validate();
	}

}
