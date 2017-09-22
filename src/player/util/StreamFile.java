package player.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;

import jouvieje.bass.utils.FileIOUtils;

public class StreamFile {
    private RandomAccessFile streamFileRaf = null;
    private RandomAccessFile metadataFileRaf = null;
    private FileIOUtils fileIoUtils = new FileIOUtils();
    private Duration durationStart = null;

    public static StreamFile openStreamFile(String filename) throws FileNotFoundException {
        StreamFile streamFile = new StreamFile();
        RandomAccessFile stream = new RandomAccessFile(filename, "rw");
        streamFile.setStreamFile(stream);
        int dotIndex = filename.lastIndexOf(".");
        String metaName = filename.substring(0, dotIndex) + ".txt";
        RandomAccessFile meta = new RandomAccessFile(metaName, "rw");
        streamFile.setMetadataFile(meta);
        return streamFile;
    }

    private void setStreamFile(RandomAccessFile streamFile) {
        this.streamFileRaf = streamFile;
    }

    private void setMetadataFile(RandomAccessFile metadataFile) {
        this.metadataFileRaf = metadataFile;
    }

    public void writeStreamData(ByteBuffer buffer, int length) throws IOException {
        if (buffer != null && length > 0) {
            if (null != streamFileRaf && null != fileIoUtils) {
                fileIoUtils.writeByteBuffer(streamFileRaf, buffer, length);
            }
        }
    }

    public void writeMetaData(String line) throws IOException {
        if (null == durationStart) {
            Instant streamStartedAt = Instant.now();
            durationStart = Duration.ofSeconds(streamStartedAt.getEpochSecond(), streamStartedAt.getNano());
        }
        if (null != metadataFileRaf && !line.isEmpty()) {
            Instant now = Instant.now();
            Duration durationNow = Duration.ofSeconds(now.getEpochSecond(), now.getNano());
            Duration delta = durationNow.minus(durationStart);
            String string = delta + " : " + line + System.getProperty("line.separator");
            metadataFileRaf.writeBytes(string);
        }
    }

    public void closeStreamFile() throws IOException {
        streamFileRaf.close();
        metadataFileRaf.close();
        durationStart = null;
    }

}
