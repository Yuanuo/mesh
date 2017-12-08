package com.gentics.mesh.storage;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.etc.config.MeshUploadOptions;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.streams.ReadStream;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.core.file.AsyncFile;
import io.vertx.rxjava.core.file.FileSystem;
import rx.Completable;
import rx.Observable;

@Singleton
public class LocalBinaryStorage extends AbstractBinaryStorage {

	private static final Logger log = LoggerFactory.getLogger(LocalBinaryStorage.class);

	@Inject
	public LocalBinaryStorage() {
	}

	@Override
	public Completable store(Observable<Buffer> stream, String uuid) {
		return Completable.defer(() -> {
			FileSystem fileSystem = FileSystem.newInstance(Mesh.vertx().fileSystem());
			String path = getFilePath(uuid);
			log.debug("Saving data for field to path {" + path + "}");
			MeshUploadOptions uploadOptions = Mesh.mesh().getOptions().getUploadOptions();
			File uploadFolder = new File(uploadOptions.getDirectory(), getSegmentedPath(uuid));

			if (!uploadFolder.exists()) {
				if (!uploadFolder.mkdirs()) {
					log.error("Failed to create target folder {" + uploadFolder.getAbsolutePath() + "}");
					throw error(BAD_REQUEST, "node_error_upload_failed");
				}

				if (log.isDebugEnabled()) {
					log.debug("Created folder {" + uploadFolder.getAbsolutePath() + "}");
				}
			}
			File targetFile = new File(uploadFolder, uuid + ".bin");

			return fileSystem.rxOpen(targetFile.getAbsolutePath(), new OpenOptions()).map(file -> {
				ReadStream<Buffer> st = RxHelper.toReadStream(stream);
				io.vertx.core.streams.Pump pump = io.vertx.core.streams.Pump.pump(st, file.getDelegate());
				pump.start();
				return file;
			}).doOnSuccess(file -> {
				file.flush();
			}).toCompletable();
			// log.error("Failed to save file to {" + targetPath + "}", error);
			// throw error(INTERNAL_SERVER_ERROR, "node_error_upload_failed", error);
		});
	}

	/**
	 * Return the absolute path to the binary data for the given uuid.
	 * 
	 * @param binaryUuid
	 * @return
	 */
	public static String getFilePath(String binaryUuid) {
		File folder = new File(Mesh.mesh().getOptions().getUploadOptions().getDirectory(), getSegmentedPath(binaryUuid));
		File binaryFile = new File(folder, binaryUuid + ".bin");
		return binaryFile.getAbsolutePath();
	}

	@Override
	public boolean exists(BinaryGraphField field) {
		String uuid = field.getBinary().getUuid();
		return new File(getFilePath(uuid)).exists();
	}

	@Override
	public Observable<Buffer> read(String binaryUuid) {
		String path = getFilePath(binaryUuid);
		Observable<Buffer> obs = FileSystem.newInstance(Mesh.vertx().fileSystem()).rxOpen(path, new OpenOptions()).toObservable().flatMap(
				AsyncFile::toObservable).map(buf -> buf.getDelegate());
		return obs;
	}

	/**
	 * Generate the segmented path for the given binary uuid.
	 * 
	 * @param binaryUuid
	 * @return
	 */
	public static String getSegmentedPath(String binaryUuid) {
		String partA = binaryUuid.substring(0, 2);
		String partB = binaryUuid.substring(2, 4);
		StringBuffer buffer = new StringBuffer();
		buffer.append(File.separator);
		buffer.append(partA);
		buffer.append(File.separator);
		buffer.append(partB);
		buffer.append(File.separator);
		return buffer.toString();
	}

	@Override
	public Completable delete(String binaryUuid) {
		String path = getFilePath(binaryUuid);
		return FileSystem.newInstance(Mesh.vertx().fileSystem()).rxDelete(path).toCompletable();
	}

}
