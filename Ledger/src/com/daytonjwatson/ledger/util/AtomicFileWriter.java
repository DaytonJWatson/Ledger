package com.daytonjwatson.ledger.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class AtomicFileWriter {
	private AtomicFileWriter() {
	}

	public static void ensureDirectory(File directory) {
		if (directory == null) {
			return;
		}
		if (!directory.exists()) {
			directory.mkdirs();
		}
	}

	public static void writeAtomically(File target, byte[] data) throws IOException {
		File parent = target.getParentFile();
		ensureDirectory(parent);
		File tmp = new File(parent, target.getName() + ".tmp");
		File backup = new File(parent, target.getName() + ".bak");
		try (FileOutputStream outputStream = new FileOutputStream(tmp)) {
			outputStream.write(data);
			outputStream.flush();
			outputStream.getFD().sync();
		}
		if (target.exists()) {
			Files.copy(target.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}
}
