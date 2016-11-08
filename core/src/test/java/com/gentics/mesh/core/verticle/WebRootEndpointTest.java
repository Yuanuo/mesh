package com.gentics.mesh.core.verticle;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.VersionReference;
import com.gentics.mesh.core.rest.node.WebRootResponse;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.LinkType;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PublishParameters;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractRestEndpointTest;
import com.gentics.mesh.util.URIUtils;

public class WebRootEndpointTest extends AbstractRestEndpointTest {

	@Test
	public void testReadBinaryNode() throws IOException {
		try (NoTx noTrx = db.noTx()) {
			Node node = content("news_2015");

			// 1. Transform the node into a binary content
			SchemaContainer container = schemaContainer("binary-content");
			node.setSchemaContainer(container);
			node.getLatestDraftFieldContainer(english()).setSchemaContainerVersion(container.getLatestVersion());
			prepareSchema(node, "image/*", "binary");
			String contentType = "application/octet-stream";
			int binaryLen = 8000;
			String fileName = "somefile.dat";

			// 2. Update the binary data
			GenericMessageResponse message = call(() -> uploadRandomData(node.getUuid(), "en", "binary", binaryLen, contentType, fileName));
			expectResponseMessage(message, "node_binary_field_updated", "binary");

			// 3. Try to resolve the path
			String path = "/News/2015/somefile.dat";
			WebRootResponse response = call(() -> getClient().webroot(PROJECT_NAME, path, new VersioningParameters().draft(),
					new NodeParameters().setResolveLinks(LinkType.FULL)));
			NodeDownloadResponse downloadResponse = response.getDownloadResponse();
			assertTrue(response.isDownload());
			assertNotNull(downloadResponse);
		}

	}

	@Test
	public void testReadFolderByPath() throws Exception {
		try (NoTx noTrx = db.noTx()) {
			Node folder = folder("2015");
			String path = "/News/2015";

			WebRootResponse restNode = call(() -> getClient().webroot(PROJECT_NAME, path, new VersioningParameters().draft()));
			assertThat(restNode.getNodeResponse()).is(folder).hasLanguage("en");
		}
	}

	@Test
	public void testReadFolderByPathAndResolveLinks() {
		try (NoTx noTrx = db.noTx()) {
			Node content = content("news_2015");

			content.getLatestDraftFieldContainer(english()).getHtml("content")
					.setHtml("<a href=\"{{mesh.link('" + content.getUuid() + "', 'en')}}\">somelink</a>");

			String path = "/News/2015/News_2015.en.html";
			WebRootResponse restNode = call(() -> getClient().webroot(PROJECT_NAME, path, new VersioningParameters().draft(),
					new NodeParameters().setResolveLinks(LinkType.FULL).setLanguages("en")));
			HtmlFieldImpl contentField = restNode.getNodeResponse().getFields().getHtmlField("content");
			assertNotNull(contentField);
			assertEquals("Check rendered content", "<a href=\"/api/v1/dummy/webroot/News/2015/News_2015.en.html\">somelink</a>",
					contentField.getHTML());
			assertThat(restNode.getNodeResponse()).is(content).hasLanguage("en");
		}
	}

	@Test
	public void testReadContentByPath() throws Exception {
		String path = "/News/2015/News_2015.en.html";

		WebRootResponse restNode = call(
				() -> getClient().webroot(PROJECT_NAME, path, new VersioningParameters().draft(), new NodeParameters().setLanguages("en", "de")));

		try (NoTx noTrx = db.noTx()) {
			Node node = content("news_2015");
			assertThat(restNode.getNodeResponse()).is(node).hasLanguage("en");
		}
	}

	@Test
	public void testReadContentWithNodeRefByPath() throws Exception {

		try (NoTx noTrx = db.noTx()) {
			Node parentNode = folder("2015");
			// Update content schema and add node field
			SchemaContainer folderSchema = schemaContainer("folder");
			Schema schema = folderSchema.getLatestVersion().getSchema();
			schema.getFields().add(FieldUtil.createNodeFieldSchema("nodeRef"));
			folderSchema.getLatestVersion().setSchema(schema);
			MeshInternal.get().serverSchemaStorage().addSchema(schema);

			// Create content which is only german
			SchemaContainer contentSchema = schemaContainer("content");
			Node node = parentNode.create(user(), contentSchema.getLatestVersion(), project());

			// Grant permissions to the node otherwise it will not be able to be loaded
			role().grantPermissions(node, GraphPermission.values());
			NodeGraphFieldContainer englishContainer = node.createGraphFieldContainer(german(), project().getLatestRelease(), user());
			englishContainer.createString("name").setString("german_name");
			englishContainer.createString("title").setString("german title");
			englishContainer.createString("displayName").setString("german displayName");
			englishContainer.createString("filename").setString("test.de.html");

			// Add node reference to node 2015
			parentNode.getLatestDraftFieldContainer(english()).createNode("nodeRef", node);

			String path = "/News/2015";
			WebRootResponse restNode = call(() -> getClient().webroot(PROJECT_NAME, path, new VersioningParameters().draft(),
					new NodeParameters().setResolveLinks(LinkType.MEDIUM).setLanguages("en", "de")));
			assertEquals("The node reference did not point to the german node.", "/dummy/News/2015/test.de.html",
					restNode.getNodeResponse().getFields().getNodeField("nodeRef").getPath());
			assertEquals("The name of the node did not match", "2015", restNode.getNodeResponse().getFields().getStringField("name").getString());

			// Again with no german fallback option (only english)
			restNode = call(() -> getClient().webroot(PROJECT_NAME, path, new VersioningParameters().draft(),
					new NodeParameters().setResolveLinks(LinkType.MEDIUM).setLanguages("en")));
			assertEquals("The node reference did not point to the 404 path.", "/dummy/error/404",
					restNode.getNodeResponse().getFields().getNodeField("nodeRef").getPath());
			assertEquals("The name of the node did not match", "2015", restNode.getNodeResponse().getFields().getStringField("name").getString());
		}

	}

	@Test
	public void testReadMultithreaded() {
		int nJobs = 200;
		String path = "/News/2015/News_2015.en.html";

		List<MeshResponse<WebRootResponse>> futures = new ArrayList<>();
		for (int i = 0; i < nJobs; i++) {
			futures.add(getClient().webroot(PROJECT_NAME, path, new VersioningParameters().draft(), new NodeParameters().setLanguages("en", "de"))
					.invoke());
		}

		for (MeshResponse<WebRootResponse> fut : futures) {
			latchFor(fut);
			assertSuccess(fut);
		}
	}

	@Test
	public void testPathWithSpaces() throws Exception {
		String[] path = new String[] { "News", "2015", "Special News_2014.en.html" };
		call(() -> getClient().webroot(PROJECT_NAME, path, new VersioningParameters().draft(), new NodeParameters().setLanguages("en", "de")));
	}

	@Test
	public void testPathWithPlus() throws Exception {
		//Test RFC3986 subdelims and an additional space and questionmark
		String newName = "20!$&'()*+,;=%3F? 15";
		String uuid = db.noTx(() -> folder("2015").getUuid());
		try (NoTx noTx = db.noTx()) {
			Node folder = folder("2015");
			folder.getGraphFieldContainer("en").getString("name").setString(newName);
		}

		String[] path = new String[] { "News", newName };
		WebRootResponse response = call(() -> getClient().webroot(PROJECT_NAME, path, new VersioningParameters().draft(),
				new NodeParameters().setLanguages("en", "de").setResolveLinks(LinkType.SHORT)));
		assertEquals(uuid, response.getNodeResponse().getUuid());
		assertEquals("/News/" + URIUtils.encodeFragment(newName), response.getNodeResponse().getPath());
	}

	@Test
	public void testReadFolderWithBogusPath() throws Exception {
		String path = "/blub";
		call(() -> getClient().webroot(PROJECT_NAME, path), NOT_FOUND, "node_not_found_for_path", path);
	}

	@Test(expected = RuntimeException.class)
	public void testReadWithEmptyPath() {
		MeshResponse<WebRootResponse> future = getClient().webroot(PROJECT_NAME, "").invoke();
		latchFor(future);
		assertSuccess(future);
		WebRootResponse response = future.result();
		assertEquals(project().getBaseNode().getUuid(), response.getNodeResponse().getUuid());
	}

	@Test
	public void testReadProjectBaseNode() {
		WebRootResponse response = call(() -> getClient().webroot(PROJECT_NAME, "/", new VersioningParameters().draft()));
		assertFalse(response.isDownload());
		try (NoTx noTrx = db.noTx()) {
			assertEquals("We expected the project basenode.", project().getBaseNode().getUuid(), response.getNodeResponse().getUuid());
		}
	}

	@Test
	public void testReadDoubleSlashes() {
		WebRootResponse response = call(() -> getClient().webroot(PROJECT_NAME, "//", new VersioningParameters().draft()));
		assertFalse(response.isDownload());
		try (NoTx noTrx = db.noTx()) {
			assertEquals("We expected the project basenode.", project().getBaseNode().getUuid(), response.getNodeResponse().getUuid());
		}
	}

	@Test
	public void testReadFolderWithLanguageFallbackInPath() {
		// Test requesting a path that contains of mixed language segments: e.g: /Fahrzeuge/Cars/auto.html
		String name = "New_in_March_2014";
		for (String path1 : Arrays.asList("News", "Neuigkeiten")) {
			for (String path2 : Arrays.asList("2014")) {
				for (String path3 : Arrays.asList("March", "März")) {
					for (String language : Arrays.asList("en", "de")) {
						WebRootResponse response = call(() -> getClient().webroot(PROJECT_NAME,
								new String[] { path1, path2, path3, name + "." + language + ".html" }, new VersioningParameters().draft()));

						assertEquals("Check response language", language, response.getNodeResponse().getLanguage());
					}
				}
			}
		}
	}

	@Test
	public void testReadFolderByPathWithoutPerm() throws Exception {
		String englishPath = "/News/2015";
		String uuid;
		try (NoTx noTrx = db.noTx()) {
			Node newsFolder = folder("2015");
			uuid = newsFolder.getUuid();
			role().revokePermissions(newsFolder, READ_PERM);
		}

		MeshResponse<WebRootResponse> future = getClient().webroot(PROJECT_NAME, englishPath, new VersioningParameters().draft()).invoke();
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", uuid);
	}

	@Test
	public void testReadContentByInvalidPath() throws Exception {
		String invalidPath = "/News/2015/no-valid-content.html";

		MeshResponse<WebRootResponse> future = getClient().webroot(PROJECT_NAME, invalidPath).invoke();
		latchFor(future);
		expectException(future, NOT_FOUND, "node_not_found_for_path", invalidPath);
	}

	@Test
	public void testReadContentByInvalidPath2() throws Exception {
		String invalidPath = "/News/no-valid-folder/no-valid-content.html";
		MeshResponse<WebRootResponse> future = getClient().webroot(PROJECT_NAME, invalidPath).invoke();
		latchFor(future);
		expectException(future, NOT_FOUND, "node_not_found_for_path", invalidPath);
	}

	@Test
	public void testRead404Page() {
		String notFoundPath = "/error/404";

		MeshResponse<WebRootResponse> future = getClient().webroot(PROJECT_NAME, notFoundPath).invoke();
		latchFor(future);
		expectException(future, NOT_FOUND, "node_not_found_for_path", notFoundPath);
	}

	/**
	 * Test reading the "not found" path /error/404, when this resolves to an existing node. We expect the node to be returned, but the status code still to be
	 * 404
	 */
	@Test
	public void testRead404Node() {
		String notFoundPath = "/error/404";

		try (NoTx noTrx = db.noTx()) {
			NodeCreateRequest createErrorFolder = new NodeCreateRequest();
			createErrorFolder.setSchema(new SchemaReference().setName("folder"));
			createErrorFolder.setParentNodeUuid(project().getBaseNode().getUuid());
			createErrorFolder.getFields().put("name", FieldUtil.createStringField("error"));
			createErrorFolder.setLanguage("en");
			NodeResponse response = call(() -> getClient().createNode(PROJECT_NAME, createErrorFolder));
			String errorNodeUuid = response.getUuid();

			NodeCreateRequest create404Node = new NodeCreateRequest();
			create404Node.setSchema(new SchemaReference().setName("content"));
			create404Node.setParentNodeUuid(errorNodeUuid);
			create404Node.getFields().put("filename", FieldUtil.createStringField("404"));
			create404Node.getFields().put("name", FieldUtil.createStringField("Error Content"));
			create404Node.getFields().put("content", FieldUtil.createStringField("An error happened"));
			create404Node.setLanguage("en");
			call(() -> getClient().createNode(PROJECT_NAME, create404Node));

			MeshResponse<WebRootResponse> webrootFuture = getClient().webroot(PROJECT_NAME, notFoundPath, new VersioningParameters().draft())
					.invoke();
			latchFor(webrootFuture);
			expectFailureMessage(webrootFuture, NOT_FOUND, null);
		}
	}

	@Test
	public void testReadPublished() {
		String path = "/News/2015";

		try (NoTx noTx = db.noTx()) {
			call(() -> getClient().takeNodeOffline(PROJECT_NAME, project().getBaseNode().getUuid(), new PublishParameters().setRecursive(true)));
		}
		// 1. Assert that published path cannot be found
		try (NoTx noTx = db.noTx()) {
			call(() -> getClient().webroot(PROJECT_NAME, path, new NodeParameters()), NOT_FOUND, "node_not_found_for_path", path);
		}

		// 2. Publish nodes
		try (NoTx noTx = db.noTx()) {
			folder("news").publish(getMockedInternalActionContext(user())).await();
			folder("2015").publish(getMockedInternalActionContext(user())).await();
		}

		// 3. Assert that published path can be found
		try (NoTx noTx = db.noTx()) {
			WebRootResponse restNode = call(() -> getClient().webroot(PROJECT_NAME, path, new NodeParameters()));
			assertThat(restNode.getNodeResponse()).is(folder("2015")).hasVersion("2.0").hasLanguage("en");
		}
	}

	@Test
	public void testReadPublishedDifferentFromDraft() {
		String publishedPath = "/News/2015";
		String draftPath = "/News_draft/2015_draft";

		// 1. Publish nodes
		db.noTx(() -> {
			folder("news").publish(getMockedInternalActionContext()).await();
			folder("2015").publish(getMockedInternalActionContext()).await();
			return null;
		});

		// 2. Change names
		db.noTx(() -> {
			updateName(folder("news"), "en", "News_draft");
			updateName(folder("2015"), "en", "2015_draft");
			return null;
		});

		// 3. Assert published path in published
		db.noTx(() -> {
			WebRootResponse restNode = call(() -> getClient().webroot(PROJECT_NAME, publishedPath, new NodeParameters()));
			assertThat(restNode.getNodeResponse()).is(folder("2015")).hasVersion("1.0").hasLanguage("en");
			return null;
		});

		// 4. Assert published path in draft
		db.noTx(() -> {
			call(() -> getClient().webroot(PROJECT_NAME, publishedPath, new VersioningParameters().draft()), NOT_FOUND, "node_not_found_for_path",
					publishedPath);
			return null;
		});

		// 5. Assert draft path in draft
		db.noTx(() -> {
			WebRootResponse restNode = call(() -> getClient().webroot(PROJECT_NAME, draftPath, new VersioningParameters().draft()));
			assertThat(restNode.getNodeResponse()).is(folder("2015")).hasVersion("1.1").hasLanguage("en");
			return null;
		});

		// 6. Assert draft path in published
		db.noTx(() -> {
			call(() -> getClient().webroot(PROJECT_NAME, draftPath, new NodeParameters()), NOT_FOUND, "node_not_found_for_path", draftPath);
			return null;
		});
	}

	@Test
	public void testReadForRelease() {
		String newReleaseName = "newrelease";
		String initialPath = "/News/2015";
		String newPath = "/News_new/2015_new";

		// 1. create new release and migrate node
		db.noTx(() -> {
			Release newRelease = project().getReleaseRoot().create(newReleaseName, user());
			meshDagger.nodeMigrationHandler().migrateNodes(newRelease).await();
			return null;
		});

		// 2. update nodes in new release
		db.noTx(() -> {
			updateName(folder("news"), "en", "News_new");
			updateName(folder("2015"), "en", "2015_new");
			return null;
		});

		// 3. Assert new names in new release
		db.noTx(() -> {
			WebRootResponse restNode = call(() -> getClient().webroot(PROJECT_NAME, newPath, new VersioningParameters().draft()));
			assertThat(restNode.getNodeResponse()).is(folder("2015")).hasVersion("1.1").hasLanguage("en");
			return null;
		});

		// 4. Assert new names in initial release
		db.noTx(() -> {
			call(() -> getClient().webroot(PROJECT_NAME, newPath,
					new VersioningParameters().draft().setRelease(project().getInitialRelease().getUuid())), NOT_FOUND, "node_not_found_for_path",
					newPath);
			return null;
		});

		// 5. Assert old names in initial release
		db.noTx(() -> {
			WebRootResponse restNode = call(() -> getClient().webroot(PROJECT_NAME, initialPath,
					new VersioningParameters().draft().setRelease(project().getInitialRelease().getUuid())));
			assertThat(restNode.getNodeResponse()).is(folder("2015")).hasVersion("1.0").hasLanguage("en");
			return null;
		});

		// 6. Assert old names in new release
		db.noTx(() -> {
			call(() -> getClient().webroot(PROJECT_NAME, initialPath, new VersioningParameters().draft()), NOT_FOUND, "node_not_found_for_path",
					initialPath);
			return null;
		});
	}

	/**
	 * Update the node name for the latest release
	 * 
	 * @param node
	 *            node
	 * @param language
	 *            language
	 * @param newName
	 *            new name
	 */
	protected void updateName(Node node, String language, String newName) {
		NodeUpdateRequest update = new NodeUpdateRequest();
		update.setLanguage(language);
		update.setVersion(
				new VersionReference(node.getGraphFieldContainer(language).getUuid(), node.getGraphFieldContainer(language).getVersion().toString()));
		update.getFields().put("name", FieldUtil.createStringField(newName));
		call(() -> getClient().updateNode(PROJECT_NAME, node.getUuid(), update));
	}

	/**
	 * Update the node name for the given release
	 * 
	 * @param node
	 *            node
	 * @param release
	 *            release
	 * @param language
	 *            language
	 * @param newName
	 *            new name
	 */
	protected void updateName(Node node, Release release, String language, String newName) {
		NodeUpdateRequest update = new NodeUpdateRequest();
		update.setLanguage(language);
		update.getFields().put("name", FieldUtil.createStringField(newName));
		call(() -> getClient().updateNode(PROJECT_NAME, node.getUuid(), update, new VersioningParameters().setRelease(release.getUuid())));
	}
}
