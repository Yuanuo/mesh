package com.gentics.mesh.core.rest.node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.rest.common.AbstractGenericNodeRestModel;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.tag.TagReference;

public class NodeResponse extends AbstractGenericNodeRestModel {

	private boolean published;

	private String language;
	private List<String> availableLanguages;

	private String path;

	private String version;

	private String parentNodeUuid;

	private List<TagReference> tags = new ArrayList<>();

	private ProjectResponse project;

	private List<String> children;

	private SchemaReference schema;

	private boolean isContainer;

	private String displayField;

	private String segmentField;

	private Map<String, Field> fields = new HashMap<>();

	public NodeResponse() {
	}

	public String getParentNodeUuid() {
		return parentNodeUuid;
	}

	public void setParentNodeUuid(String parentNodeUuid) {
		this.parentNodeUuid = parentNodeUuid;
	}

	public List<TagReference> getTags() {
		return tags;
	}

	public void setSchema(SchemaReference schema) {
		this.schema = schema;
	}

	public SchemaReference getSchema() {
		return schema;
	}

	public List<String> getChildren() {
		return children;
	}

	public void setChildren(List<String> children) {
		this.children = children;
	}

	public boolean isContainer() {
		return isContainer;
	}

	public void setContainer(boolean isContainer) {
		this.isContainer = isContainer;
	}

	public boolean isPublished() {
		return published;
	}

	public void setPublished(boolean published) {
		this.published = published;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getDisplayField() {
		return displayField;
	}

	public void setDisplayField(String displayField) {
		this.displayField = displayField;
	}

	public String getSegmentField() {
		return segmentField;
	}

	public void setSegmentField(String segmentField) {
		this.segmentField = segmentField;
	}

	public ProjectResponse getProject() {
		return project;
	}

	public void setProject(ProjectResponse project) {
		this.project = project;
	}

	@SuppressWarnings("unchecked")
	public <T extends Field> T getField(String key, Class<T> classOfT) {
		return (T) getFields().get(key);
	}

	@SuppressWarnings("unchecked")
	public <T extends Field> T getField(String key) {
		return (T) getFields().get(key);
	}

	public Map<String, Field> getFields() {
		return fields;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public List<String> getAvailableLanguages() {
		return availableLanguages;
	}

	public void setAvailableLanguages(List<String> availableLanguages) {
		this.availableLanguages = availableLanguages;
	}

}
