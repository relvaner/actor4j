package io.actor4j.dataformat.bson.utils;

import java.nio.ByteBuffer;

import org.bson.BsonBinaryReader;
import org.bson.BsonBinaryWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.EncoderContext;
import org.bson.io.BasicOutputBuffer;

// Ross Lawley, @See: https://gist.github.com/rozza/9c94808ed5b4f1edca75
public final class BsonUtils {
	private static final Codec<Document> DOCUMENT_CODEC = new DocumentCodec();
	
	public static byte[] encode(Document document) {	
		BasicOutputBuffer buffer = new BasicOutputBuffer();
		DOCUMENT_CODEC.encode(new BsonBinaryWriter(buffer), document, EncoderContext.builder().isEncodingCollectibleDocument(true).build());
		
		return buffer.toByteArray();
	}
		
	public static Document decode(byte[] src) {
		return DOCUMENT_CODEC.decode(new BsonBinaryReader(ByteBuffer.wrap(src)), DecoderContext.builder().build());
	}
}
