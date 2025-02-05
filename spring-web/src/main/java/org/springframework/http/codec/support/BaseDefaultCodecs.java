/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.codec.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.core.SpringProperties;
import org.springframework.core.codec.AbstractDataBufferDecoder;
import org.springframework.core.codec.ByteArrayDecoder;
import org.springframework.core.codec.ByteArrayEncoder;
import org.springframework.core.codec.ByteBufferDecoder;
import org.springframework.core.codec.ByteBufferEncoder;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.DataBufferDecoder;
import org.springframework.core.codec.DataBufferEncoder;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.Netty5BufferDecoder;
import org.springframework.core.codec.Netty5BufferEncoder;
import org.springframework.core.codec.NettyByteBufDecoder;
import org.springframework.core.codec.NettyByteBufEncoder;
import org.springframework.core.codec.ResourceDecoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.FormHttpMessageReader;
import org.springframework.http.codec.FormHttpMessageWriter;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ResourceHttpMessageReader;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.http.codec.ServerSentEventHttpMessageReader;
import org.springframework.http.codec.ServerSentEventHttpMessageWriter;
import org.springframework.http.codec.cbor.KotlinSerializationCborDecoder;
import org.springframework.http.codec.cbor.KotlinSerializationCborEncoder;
import org.springframework.http.codec.json.AbstractJackson2Decoder;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.json.Jackson2SmileDecoder;
import org.springframework.http.codec.json.Jackson2SmileEncoder;
import org.springframework.http.codec.json.KotlinSerializationJsonDecoder;
import org.springframework.http.codec.json.KotlinSerializationJsonEncoder;
import org.springframework.http.codec.multipart.DefaultPartHttpMessageReader;
import org.springframework.http.codec.multipart.MultipartHttpMessageReader;
import org.springframework.http.codec.multipart.MultipartHttpMessageWriter;
import org.springframework.http.codec.multipart.PartEventHttpMessageReader;
import org.springframework.http.codec.protobuf.KotlinSerializationProtobufDecoder;
import org.springframework.http.codec.protobuf.KotlinSerializationProtobufEncoder;
import org.springframework.http.codec.protobuf.ProtobufDecoder;
import org.springframework.http.codec.protobuf.ProtobufEncoder;
import org.springframework.http.codec.protobuf.ProtobufHttpMessageWriter;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Default implementation of {@link CodecConfigurer.DefaultCodecs} that serves
 * as a base for client and server specific variants.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
class BaseDefaultCodecs implements CodecConfigurer.DefaultCodecs, CodecConfigurer.DefaultCodecConfig {

	/**
	 * Boolean flag controlled by a {@code spring.xml.ignore} system property that instructs Spring to
	 * ignore XML, i.e. to not initialize the XML-related infrastructure.
	 * <p>The default is "false".
	 */
	private static final boolean shouldIgnoreXml = SpringProperties.getFlag("spring.xml.ignore");

	static final boolean jackson2Present;

	private static final boolean jackson2SmilePresent;

	private static final boolean jaxb2Present;

	private static final boolean protobufPresent;

	static final boolean synchronossMultipartPresent;

	static final boolean nettyByteBufPresent;

	static final boolean netty5BufferPresent;

	static final boolean kotlinSerializationCborPresent;

	static final boolean kotlinSerializationJsonPresent;

	static final boolean kotlinSerializationProtobufPresent;

	static {
		ClassLoader classLoader = BaseCodecConfigurer.class.getClassLoader();
		jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
						ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
		jackson2SmilePresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory", classLoader);
		jaxb2Present = ClassUtils.isPresent("jakarta.xml.bind.Binder", classLoader);
		protobufPresent = ClassUtils.isPresent("com.google.protobuf.Message", classLoader);
		synchronossMultipartPresent = ClassUtils.isPresent("org.synchronoss.cloud.nio.multipart.NioMultipartParser", classLoader);
		nettyByteBufPresent = ClassUtils.isPresent("io.netty.buffer.ByteBuf", classLoader);
		netty5BufferPresent = ClassUtils.isPresent("io.netty5.buffer.Buffer", classLoader);
		kotlinSerializationCborPresent = ClassUtils.isPresent("kotlinx.serialization.cbor.Cbor", classLoader);
		kotlinSerializationJsonPresent = ClassUtils.isPresent("kotlinx.serialization.json.Json", classLoader);
		kotlinSerializationProtobufPresent = ClassUtils.isPresent("kotlinx.serialization.protobuf.ProtoBuf", classLoader);
	}


	@Nullable
	private Decoder<?> jackson2JsonDecoder;

	@Nullable
	private Encoder<?> jackson2JsonEncoder;

	@Nullable
	private Encoder<?> jackson2SmileEncoder;

	@Nullable
	private Decoder<?> jackson2SmileDecoder;

	@Nullable
	private Decoder<?> protobufDecoder;

	@Nullable
	private Encoder<?> protobufEncoder;

	@Nullable
	private Decoder<?> jaxb2Decoder;

	@Nullable
	private Encoder<?> jaxb2Encoder;

	@Nullable
	private Decoder<?> kotlinSerializationCborDecoder;

	@Nullable
	private Encoder<?> kotlinSerializationCborEncoder;

	@Nullable
	private Decoder<?> kotlinSerializationJsonDecoder;

	@Nullable
	private Encoder<?> kotlinSerializationJsonEncoder;

	@Nullable
	private Decoder<?> kotlinSerializationProtobufDecoder;

	@Nullable
	private Encoder<?> kotlinSerializationProtobufEncoder;

	@Nullable
	private Consumer<Object> codecConsumer;

	@Nullable
	private Integer maxInMemorySize;

	@Nullable
	private Boolean enableLoggingRequestDetails;

	private boolean registerDefaults = true;


	// The default reader and writer instances to use

	private final List<HttpMessageReader<?>> typedReaders = new ArrayList<>();

	private final List<HttpMessageReader<?>> objectReaders = new ArrayList<>();

	private final List<HttpMessageWriter<?>> typedWriters = new ArrayList<>();

	private final List<HttpMessageWriter<?>> objectWriters = new ArrayList<>();


	BaseDefaultCodecs() {
		initReaders();
		initWriters();
	}

	/**
	 * Reset and initialize typed readers and object readers.
	 * @since 5.3.3
	 */
	protected void initReaders() {
		initTypedReaders();
		initObjectReaders();
	}

	/**
	 * Reset and initialize typed writers and object writers.
	 * @since 5.3.3
	 */
	protected void initWriters() {
		initTypedWriters();
		initObjectWriters();
	}

	/**
	 * Create a deep copy of the given {@link BaseDefaultCodecs}.
	 */
	protected BaseDefaultCodecs(BaseDefaultCodecs other) {
		this.jackson2JsonDecoder = other.jackson2JsonDecoder;
		this.jackson2JsonEncoder = other.jackson2JsonEncoder;
		this.jackson2SmileDecoder = other.jackson2SmileDecoder;
		this.jackson2SmileEncoder = other.jackson2SmileEncoder;
		this.protobufDecoder = other.protobufDecoder;
		this.protobufEncoder = other.protobufEncoder;
		this.jaxb2Decoder = other.jaxb2Decoder;
		this.jaxb2Encoder = other.jaxb2Encoder;
		this.kotlinSerializationCborDecoder = other.kotlinSerializationCborDecoder;
		this.kotlinSerializationCborEncoder = other.kotlinSerializationCborEncoder;
		this.kotlinSerializationJsonDecoder = other.kotlinSerializationJsonDecoder;
		this.kotlinSerializationJsonEncoder = other.kotlinSerializationJsonEncoder;
		this.kotlinSerializationProtobufDecoder = other.kotlinSerializationProtobufDecoder;
		this.kotlinSerializationProtobufEncoder = other.kotlinSerializationProtobufEncoder;
		this.codecConsumer = other.codecConsumer;
		this.maxInMemorySize = other.maxInMemorySize;
		this.enableLoggingRequestDetails = other.enableLoggingRequestDetails;
		this.registerDefaults = other.registerDefaults;
		this.typedReaders.addAll(other.typedReaders);
		this.objectReaders.addAll(other.objectReaders);
		this.typedWriters.addAll(other.typedWriters);
		this.objectWriters.addAll(other.objectWriters);
	}

	@Override
	public void jackson2JsonDecoder(Decoder<?> decoder) {
		this.jackson2JsonDecoder = decoder;
		initObjectReaders();
	}

	@Override
	public void jackson2JsonEncoder(Encoder<?> encoder) {
		this.jackson2JsonEncoder = encoder;
		initObjectWriters();
		initTypedWriters();
	}

	@Override
	public void jackson2SmileDecoder(Decoder<?> decoder) {
		this.jackson2SmileDecoder = decoder;
		initObjectReaders();
	}

	@Override
	public void jackson2SmileEncoder(Encoder<?> encoder) {
		this.jackson2SmileEncoder = encoder;
		initObjectWriters();
		initTypedWriters();
	}

	@Override
	public void protobufDecoder(Decoder<?> decoder) {
		this.protobufDecoder = decoder;
		initTypedReaders();
	}

	@Override
	public void protobufEncoder(Encoder<?> encoder) {
		this.protobufEncoder = encoder;
		initTypedWriters();
	}

	@Override
	public void jaxb2Decoder(Decoder<?> decoder) {
		this.jaxb2Decoder = decoder;
		initObjectReaders();
	}

	@Override
	public void jaxb2Encoder(Encoder<?> encoder) {
		this.jaxb2Encoder = encoder;
		initObjectWriters();
	}

	@Override
	public void kotlinSerializationCborDecoder(Decoder<?> decoder) {
		this.kotlinSerializationCborDecoder = decoder;
		initObjectReaders();
	}

	@Override
	public void kotlinSerializationCborEncoder(Encoder<?> encoder) {
		this.kotlinSerializationCborEncoder = encoder;
		initObjectWriters();
	}

	@Override
	public void kotlinSerializationJsonDecoder(Decoder<?> decoder) {
		this.kotlinSerializationJsonDecoder = decoder;
		initObjectReaders();
	}

	@Override
	public void kotlinSerializationJsonEncoder(Encoder<?> encoder) {
		this.kotlinSerializationJsonEncoder = encoder;
		initObjectWriters();
	}

	@Override
	public void kotlinSerializationProtobufDecoder(Decoder<?> decoder) {
		this.kotlinSerializationProtobufDecoder = decoder;
		initObjectReaders();
	}

	@Override
	public void kotlinSerializationProtobufEncoder(Encoder<?> encoder) {
		this.kotlinSerializationProtobufEncoder = encoder;
		initObjectWriters();
	}

	@Override
	public void configureDefaultCodec(Consumer<Object> codecConsumer) {
		this.codecConsumer = (this.codecConsumer != null ?
				this.codecConsumer.andThen(codecConsumer) : codecConsumer);
		initReaders();
		initWriters();
	}

	@Override
	public void maxInMemorySize(int byteCount) {
		if (!ObjectUtils.nullSafeEquals(this.maxInMemorySize, byteCount)) {
			this.maxInMemorySize = byteCount;
			initReaders();
		}
	}

	@Override
	@Nullable
	public Integer maxInMemorySize() {
		return this.maxInMemorySize;
	}

	@Override
	public void enableLoggingRequestDetails(boolean enable) {
		if (!ObjectUtils.nullSafeEquals(this.enableLoggingRequestDetails, enable)) {
			this.enableLoggingRequestDetails = enable;
			initReaders();
			initWriters();
		}
	}

	@Override
	@Nullable
	public Boolean isEnableLoggingRequestDetails() {
		return this.enableLoggingRequestDetails;
	}

	/**
	 * Delegate method used from {@link BaseCodecConfigurer#registerDefaults}.
	 */
	void registerDefaults(boolean registerDefaults) {
		if (this.registerDefaults != registerDefaults) {
			this.registerDefaults = registerDefaults;
			initReaders();
			initWriters();
		}
	}


	/**
	 * Return readers that support specific types.
	 */
	final List<HttpMessageReader<?>> getTypedReaders() {
		return this.typedReaders;
	}

	/**
	 * Reset and initialize typed readers.
	 * @since 5.3.3
	 */
	protected void initTypedReaders() {
		this.typedReaders.clear();
		if (!this.registerDefaults) {
			return;
		}
		addCodec(this.typedReaders, new DecoderHttpMessageReader<>(new ByteArrayDecoder()));
		addCodec(this.typedReaders, new DecoderHttpMessageReader<>(new ByteBufferDecoder()));
		addCodec(this.typedReaders, new DecoderHttpMessageReader<>(new DataBufferDecoder()));
		if (nettyByteBufPresent) {
			addCodec(this.typedReaders, new DecoderHttpMessageReader<>(new NettyByteBufDecoder()));
		}
		if (netty5BufferPresent) {
			addCodec(this.typedReaders, new DecoderHttpMessageReader<>(new Netty5BufferDecoder()));
		}
		addCodec(this.typedReaders, new ResourceHttpMessageReader(new ResourceDecoder()));
		addCodec(this.typedReaders, new DecoderHttpMessageReader<>(StringDecoder.textPlainOnly()));
		if (protobufPresent) {
			addCodec(this.typedReaders, new DecoderHttpMessageReader<>(this.protobufDecoder != null ?
					(ProtobufDecoder) this.protobufDecoder : new ProtobufDecoder()));
		}
		else if (kotlinSerializationProtobufPresent) {
			addCodec(this.typedReaders, new DecoderHttpMessageReader<>(this.kotlinSerializationProtobufDecoder != null ?
					(KotlinSerializationProtobufDecoder) this.kotlinSerializationProtobufDecoder : new KotlinSerializationProtobufDecoder()));
		}
		addCodec(this.typedReaders, new FormHttpMessageReader());

		// client vs server..
		extendTypedReaders(this.typedReaders);
	}

	/**
	 * Initialize a codec and add it to the List.
	 * @since 5.1.13
	 */
	protected <T> void addCodec(List<T> codecs, T codec) {
		initCodec(codec);
		codecs.add(codec);
	}

	/**
	 * Apply {@link #maxInMemorySize()} and {@link #enableLoggingRequestDetails},
	 * if configured by the application, to the given codec , including any
	 * codec it contains.
	 */
	@SuppressWarnings("rawtypes")
	private void initCodec(@Nullable Object codec) {
		if (codec instanceof DecoderHttpMessageReader) {
			codec = ((DecoderHttpMessageReader) codec).getDecoder();
		}
		else if (codec instanceof EncoderHttpMessageWriter) {
			codec = ((EncoderHttpMessageWriter<?>) codec).getEncoder();
		}

		if (codec == null) {
			return;
		}

		Integer size = this.maxInMemorySize;
		if (size != null) {
			if (codec instanceof AbstractDataBufferDecoder) {
				((AbstractDataBufferDecoder<?>) codec).setMaxInMemorySize(size);
			}
			if (protobufPresent) {
				if (codec instanceof ProtobufDecoder) {
					((ProtobufDecoder) codec).setMaxMessageSize(size);
				}
			}
			if (kotlinSerializationCborPresent) {
				if (codec instanceof KotlinSerializationCborDecoder) {
					((KotlinSerializationCborDecoder) codec).setMaxInMemorySize(size);
				}
			}
			if (kotlinSerializationJsonPresent) {
				if (codec instanceof KotlinSerializationJsonDecoder) {
					((KotlinSerializationJsonDecoder) codec).setMaxInMemorySize(size);
				}
			}
			if (kotlinSerializationProtobufPresent) {
				if (codec instanceof KotlinSerializationProtobufDecoder) {
					((KotlinSerializationProtobufDecoder) codec).setMaxInMemorySize(size);
				}
			}
			if (jackson2Present) {
				if (codec instanceof AbstractJackson2Decoder) {
					((AbstractJackson2Decoder) codec).setMaxInMemorySize(size);
				}
			}
			if (jaxb2Present && !shouldIgnoreXml) {
				if (codec instanceof Jaxb2XmlDecoder) {
					((Jaxb2XmlDecoder) codec).setMaxInMemorySize(size);
				}
			}
			if (codec instanceof FormHttpMessageReader) {
				((FormHttpMessageReader) codec).setMaxInMemorySize(size);
			}
			if (codec instanceof ServerSentEventHttpMessageReader) {
				((ServerSentEventHttpMessageReader) codec).setMaxInMemorySize(size);
			}
			if (codec instanceof DefaultPartHttpMessageReader) {
				((DefaultPartHttpMessageReader) codec).setMaxInMemorySize(size);
			}
			if (codec instanceof PartEventHttpMessageReader) {
				((PartEventHttpMessageReader) codec).setMaxInMemorySize(size);
			}
		}

		Boolean enable = this.enableLoggingRequestDetails;
		if (enable != null) {
			if (codec instanceof FormHttpMessageReader) {
				((FormHttpMessageReader) codec).setEnableLoggingRequestDetails(enable);
			}
			if (codec instanceof MultipartHttpMessageReader) {
				((MultipartHttpMessageReader) codec).setEnableLoggingRequestDetails(enable);
			}
			if (codec instanceof DefaultPartHttpMessageReader) {
				((DefaultPartHttpMessageReader) codec).setEnableLoggingRequestDetails(enable);
			}
			if (codec instanceof PartEventHttpMessageReader) {
				((PartEventHttpMessageReader) codec).setEnableLoggingRequestDetails(enable);
			}
			if (codec instanceof FormHttpMessageWriter) {
				((FormHttpMessageWriter) codec).setEnableLoggingRequestDetails(enable);
			}
			if (codec instanceof MultipartHttpMessageWriter) {
				((MultipartHttpMessageWriter) codec).setEnableLoggingRequestDetails(enable);
			}
		}

		if (this.codecConsumer != null) {
			this.codecConsumer.accept(codec);
		}

		// Recurse for nested codecs
		if (codec instanceof MultipartHttpMessageReader) {
			initCodec(((MultipartHttpMessageReader) codec).getPartReader());
		}
		else if (codec instanceof MultipartHttpMessageWriter) {
			initCodec(((MultipartHttpMessageWriter) codec).getFormWriter());
		}
		else if (codec instanceof ServerSentEventHttpMessageReader) {
			initCodec(((ServerSentEventHttpMessageReader) codec).getDecoder());
		}
		else if (codec instanceof ServerSentEventHttpMessageWriter) {
			initCodec(((ServerSentEventHttpMessageWriter) codec).getEncoder());
		}
	}

	/**
	 * Hook for client or server specific typed readers.
	 */
	protected void extendTypedReaders(List<HttpMessageReader<?>> typedReaders) {
	}

	/**
	 * Return Object readers (JSON, XML, SSE).
	 */
	final List<HttpMessageReader<?>> getObjectReaders() {
		return this.objectReaders;
	}

	/**
	 * Reset and initialize object readers.
	 * @since 5.3.3
	 */
	protected void initObjectReaders() {
		this.objectReaders.clear();
		if (!this.registerDefaults) {
			return;
		}
		if (kotlinSerializationCborPresent) {
			addCodec(this.objectReaders, new DecoderHttpMessageReader<>(this.kotlinSerializationCborDecoder != null ?
					(KotlinSerializationCborDecoder) this.kotlinSerializationCborDecoder :
					new KotlinSerializationCborDecoder()));
		}
		if (kotlinSerializationJsonPresent) {
			addCodec(this.objectReaders, new DecoderHttpMessageReader<>(getKotlinSerializationJsonDecoder()));
		}
		if (kotlinSerializationProtobufPresent) {
			addCodec(this.objectReaders,
					new DecoderHttpMessageReader<>(this.kotlinSerializationProtobufDecoder != null ?
							(KotlinSerializationProtobufDecoder) this.kotlinSerializationProtobufDecoder :
							new KotlinSerializationProtobufDecoder()));
		}
		if (jackson2Present) {
			addCodec(this.objectReaders, new DecoderHttpMessageReader<>(getJackson2JsonDecoder()));
		}
		if (jackson2SmilePresent) {
			addCodec(this.objectReaders, new DecoderHttpMessageReader<>(this.jackson2SmileDecoder != null ?
					(Jackson2SmileDecoder) this.jackson2SmileDecoder : new Jackson2SmileDecoder()));
		}
		if (jaxb2Present && !shouldIgnoreXml) {
			addCodec(this.objectReaders, new DecoderHttpMessageReader<>(this.jaxb2Decoder != null ?
					(Jaxb2XmlDecoder) this.jaxb2Decoder : new Jaxb2XmlDecoder()));
		}

		// client vs server..
		extendObjectReaders(this.objectReaders);
	}

	/**
	 * Hook for client or server specific Object readers.
	 */
	protected void extendObjectReaders(List<HttpMessageReader<?>> objectReaders) {
	}

	/**
	 * Return readers that need to be at the end, after all others.
	 */
	final List<HttpMessageReader<?>> getCatchAllReaders() {
		if (!this.registerDefaults) {
			return Collections.emptyList();
		}
		List<HttpMessageReader<?>> readers = new ArrayList<>();
		addCodec(readers, new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
		return readers;
	}

	/**
	 * Return all writers that support specific types.
	 */
	final List<HttpMessageWriter<?>> getTypedWriters() {
		return this.typedWriters;
	}

	/**
	 * Reset and initialize typed writers.
	 * @since 5.3.3
	 */
	protected void initTypedWriters() {
		this.typedWriters.clear();
		if (!this.registerDefaults) {
			return;
		}
		this.typedWriters.addAll(getBaseTypedWriters());
		extendTypedWriters(this.typedWriters);
	}

	/**
	 * Return "base" typed writers only, i.e. common to client and server.
	 */
	final List<HttpMessageWriter<?>> getBaseTypedWriters() {
		if (!this.registerDefaults) {
			return Collections.emptyList();
		}
		List<HttpMessageWriter<?>> writers = new ArrayList<>();
		addCodec(writers, new EncoderHttpMessageWriter<>(new ByteArrayEncoder()));
		addCodec(writers, new EncoderHttpMessageWriter<>(new ByteBufferEncoder()));
		addCodec(writers, new EncoderHttpMessageWriter<>(new DataBufferEncoder()));
		if (nettyByteBufPresent) {
			addCodec(writers, new EncoderHttpMessageWriter<>(new NettyByteBufEncoder()));
		}
		if (netty5BufferPresent) {
			addCodec(writers, new EncoderHttpMessageWriter<>(new Netty5BufferEncoder()));
		}
		addCodec(writers, new ResourceHttpMessageWriter());
		addCodec(writers, new EncoderHttpMessageWriter<>(CharSequenceEncoder.textPlainOnly()));
		if (protobufPresent) {
			addCodec(writers, new ProtobufHttpMessageWriter(this.protobufEncoder != null ?
					(ProtobufEncoder) this.protobufEncoder : new ProtobufEncoder()));
		}
		return writers;
	}

	/**
	 * Hook for client or server specific typed writers.
	 */
	protected void extendTypedWriters(List<HttpMessageWriter<?>> typedWriters) {
	}

	/**
	 * Return Object writers (JSON, XML, SSE).
	 */
	final List<HttpMessageWriter<?>> getObjectWriters() {
		return this.objectWriters;
	}

	/**
	 * Reset and initialize object writers.
	 * @since 5.3.3
	 */
	protected void initObjectWriters() {
		this.objectWriters.clear();
		if (!this.registerDefaults) {
			return;
		}
		this.objectWriters.addAll(getBaseObjectWriters());
		extendObjectWriters(this.objectWriters);
	}

	/**
	 * Return "base" object writers only, i.e. common to client and server.
	 */
	final List<HttpMessageWriter<?>> getBaseObjectWriters() {
		List<HttpMessageWriter<?>> writers = new ArrayList<>();
		if (kotlinSerializationCborPresent) {
			addCodec(writers, new EncoderHttpMessageWriter<>(this.kotlinSerializationCborEncoder != null ?
					(KotlinSerializationCborEncoder) this.kotlinSerializationCborEncoder :
					new KotlinSerializationCborEncoder()));
		}
		if (kotlinSerializationJsonPresent) {
			addCodec(writers, new EncoderHttpMessageWriter<>(getKotlinSerializationJsonEncoder()));
		}
		if (kotlinSerializationProtobufPresent) {
			addCodec(writers, new EncoderHttpMessageWriter<>(this.kotlinSerializationProtobufEncoder != null ?
					(KotlinSerializationProtobufEncoder) this.kotlinSerializationProtobufEncoder :
					new KotlinSerializationProtobufEncoder()));
		}
		if (jackson2Present) {
			addCodec(writers, new EncoderHttpMessageWriter<>(getJackson2JsonEncoder()));
		}
		if (jackson2SmilePresent) {
			addCodec(writers, new EncoderHttpMessageWriter<>(this.jackson2SmileEncoder != null ?
					(Jackson2SmileEncoder) this.jackson2SmileEncoder : new Jackson2SmileEncoder()));
		}
		if (jaxb2Present && !shouldIgnoreXml) {
			addCodec(writers, new EncoderHttpMessageWriter<>(this.jaxb2Encoder != null ?
					(Jaxb2XmlEncoder) this.jaxb2Encoder : new Jaxb2XmlEncoder()));
		}
		return writers;
	}

	/**
	 * Hook for client or server specific Object writers.
	 */
	protected void extendObjectWriters(List<HttpMessageWriter<?>> objectWriters) {
	}

	/**
	 * Return writers that need to be at the end, after all others.
	 */
	List<HttpMessageWriter<?>> getCatchAllWriters() {
		if (!this.registerDefaults) {
			return Collections.emptyList();
		}
		List<HttpMessageWriter<?>> result = new ArrayList<>();
		result.add(new EncoderHttpMessageWriter<>(CharSequenceEncoder.allMimeTypes()));
		return result;
	}

	void applyDefaultConfig(BaseCodecConfigurer.DefaultCustomCodecs customCodecs) {
		applyDefaultConfig(customCodecs.getTypedReaders());
		applyDefaultConfig(customCodecs.getObjectReaders());
		applyDefaultConfig(customCodecs.getTypedWriters());
		applyDefaultConfig(customCodecs.getObjectWriters());
		customCodecs.getDefaultConfigConsumers().forEach(consumer -> consumer.accept(this));
	}

	private void applyDefaultConfig(Map<?, Boolean> readers) {
		readers.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.forEach(this::initCodec);
	}


	// Accessors for use in subclasses...

	protected Decoder<?> getJackson2JsonDecoder() {
		if (this.jackson2JsonDecoder == null) {
			this.jackson2JsonDecoder = new Jackson2JsonDecoder();
		}
		return this.jackson2JsonDecoder;
	}

	protected Encoder<?> getJackson2JsonEncoder() {
		if (this.jackson2JsonEncoder == null) {
			this.jackson2JsonEncoder = new Jackson2JsonEncoder();
		}
		return this.jackson2JsonEncoder;
	}

	protected Decoder<?> getKotlinSerializationJsonDecoder() {
		if (this.kotlinSerializationJsonDecoder == null) {
			this.kotlinSerializationJsonDecoder = new KotlinSerializationJsonDecoder();
		}
		return this.kotlinSerializationJsonDecoder;
	}

	protected Encoder<?> getKotlinSerializationJsonEncoder() {
		if (this.kotlinSerializationJsonEncoder == null) {
			this.kotlinSerializationJsonEncoder = new KotlinSerializationJsonEncoder();
		}
		return this.kotlinSerializationJsonEncoder;
	}

}
