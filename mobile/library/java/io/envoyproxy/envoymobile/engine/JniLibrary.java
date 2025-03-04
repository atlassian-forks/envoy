package io.envoyproxy.envoymobile.engine;

import io.envoyproxy.envoymobile.engine.types.EnvoyEventTracker;
import io.envoyproxy.envoymobile.engine.types.EnvoyLogger;
import io.envoyproxy.envoymobile.engine.types.EnvoyOnEngineRunning;

import java.nio.ByteBuffer;

public class JniLibrary {

  private static String envoyLibraryName = "envoy_jni";

  // Internal reference to helper object used to load and initialize the native
  // library.
  // Volatile to ensure double-checked locking works correctly.
  private static volatile JavaLoader loader = null;

  // Load test libraries based on the jvm_flag `envoy_jni_library_name`.
  // WARNING: This should only be used for testing.
  public static void loadTestLibrary() {
    if (System.getProperty("envoy_jni_library_name") != null) {
      envoyLibraryName = System.getProperty("envoy_jni_library_name");
    }
  }

  // Load and initialize Envoy and its dependencies, but only once.
  public static void load() {
    if (loader != null) {
      return;
    }

    synchronized (JavaLoader.class) {
      if (loader != null) {
        return;
      }

      loader = new JavaLoader();
    }
  }

  // Private helper class used by the load method to ensure the native library and
  // its
  // dependencies are loaded and initialized at most once.
  private static class JavaLoader {
    private JavaLoader() { System.loadLibrary(envoyLibraryName); }
  }

  /**
   * Initialize an underlying HTTP stream.
   *
   * @param engine, handle to the engine that will manage this stream.
   * @return long, handle to the underlying stream.
   */
  protected static native long initStream(long engine);

  /**
   * Open an underlying HTTP stream. Note: Streams must be started before other
   * other interaction can can occur.
   *
   * @param engine,  handle to the stream's associated engine.
   * @param stream,  handle to the stream to be started.
   * @param context, context that contains dispatch logic to fire callbacks
   *                 callbacks.
   * @param explicitFlowControl, whether explicit flow control should be enabled
   *                             for the stream.
   * @return envoy_stream, with a stream handle and a success status, or a failure
   * status.
   */
  protected static native int startStream(long engine, long stream, JvmCallbackContext context,
                                          boolean explicitFlowControl);

  /**
   * Send headers over an open HTTP stream. This method can be invoked once and
   * needs to be called before send_data.
   *
   * @param engine,    the stream's associated engine.
   * @param stream,    the stream to send headers over.
   * @param headers,   the headers to send.
   * @param endStream, supplies whether this is headers only.
   * @return int, the resulting status of the operation.
   */
  protected static native int sendHeaders(long engine, long stream, byte[][] headers,
                                          boolean endStream);

  /**
   * Send data over an open HTTP stream. This method can be invoked multiple
   * times.
   *
   * @param engine,    the stream's associated engine.
   * @param stream,    the stream to send data over.
   * @param data,      the data to send.
   * @param length,    the size in bytes of the data to send. 0 <= length <= data.length
   * @param endStream, supplies whether this is the last data in the stream.
   * @return int,      the resulting status of the operation.
   */
  protected static native int sendDataByteArray(long engine, long stream, byte[] data, int length,
                                                boolean endStream);

  /**
   * Send data over an open HTTP stream. This method can be invoked multiple
   * times.
   *
   * @param engine,    the stream's associated engine.
   * @param stream,    the stream to send data over.
   * @param data,      the data to send; must be a <b>direct</b> ByteBuffer.
   * @param length,    the size in bytes of the data to send. 0 <= length <= data.capacity()
   * @param endStream, supplies whether this is the last data in the stream.
   * @return int,      the resulting status of the operation.
   */
  protected static native int sendData(long engine, long stream, ByteBuffer data, int length,
                                       boolean endStream);

  /**
   * Read data from the response stream. Returns immediately.
   * Has no effect if explicit flow control is not enabled.
   *
   * @param engine,    the stream's associated engine.
   * @param stream,    the stream.
   * @param byteCount, Maximum number of bytes that may be be passed by the next data callback.
   * @return int, the resulting status of the operation.
   */
  protected static native int readData(long engine, long stream, long byteCount);

  /**
   * Send trailers over an open HTTP stream. This method can only be invoked once
   * per stream. Note that this method implicitly ends the stream.
   *
   * @param engine,   the stream's associated engine.
   * @param stream,   the stream to send trailers over.
   * @param trailers, the trailers to send.
   * @return int, the resulting status of the operation.
   */
  protected static native int sendTrailers(long engine, long stream, byte[][] trailers);

  /**
   * Detach all callbacks from a stream and send an interrupt upstream if
   * supported by transport.
   *
   * @param engine, the stream's associated engine.
   * @param stream, the stream to evict.
   * @return int, the resulting status of the operation.
   */
  protected static native int resetStream(long engine, long stream);

  /**
   * Register a factory for creating platform filter instances for each HTTP stream.
   *
   * @param filterName, unique name identifying this filter in the chain.
   * @param context,    context containing logic necessary to invoke a new filter instance.
   * @return int, the resulting status of the operation.
   */
  protected static native int registerFilterFactory(String filterName,
                                                    JvmFilterFactoryContext context);

  // Native entry point

  /**
   * Initialize an engine for handling network streams.
   *
   * @param runningCallback, called when the engine finishes its async startup and begins running.
   * @param logger,          the logging interface.
   * @param eventTracker     the event tracking interface.
   * @return envoy_engine_t, handle to the underlying engine.
   */
  protected static native long initEngine(EnvoyOnEngineRunning runningCallback, EnvoyLogger logger,
                                          EnvoyEventTracker eventTracker);

  /**
   * External entry point for library.
   *
   * @param engine,          the engine to run.
   * @param config,          the configuration blob to run envoy with.
   * @param logLevel,        the logging level to run envoy with.
   * @return int, the resulting status of the operation.
   */
  protected static native int runEngine(long engine, String config, String logLevel);

  /**
   * Terminate the engine.
   *
   * @param engine handle for the engine to terminate.
   */
  protected static native void terminateEngine(long engine);

  // Other native methods

  /**
   * Provides a default configuration template that may be used for starting
   * Envoy.
   *
   * @return A template that may be used as a starting point for constructing
   * configurations.
   */
  public static native String configTemplate();

  /**
   * Increment a counter with the given count.
   *
   * @param engine,  handle to the engine that owns the counter.
   * @param elements Elements of the counter stat.
   * @param tags Tags of the counter.
   * @param count Amount to add to the counter.
   * @return A status indicating if the action was successful.
   */
  protected static native int recordCounterInc(long engine, String elements, byte[][] tags,
                                               int count);

  /**
   * Flush the stats sinks outside of a flushing interval.
   * Note: stat flushing is done asynchronously, this function will never block.
   * This is a noop if called before the underlying EnvoyEngine has started.
   *
   * @param engine engine whose stats should be flushed.
   */
  protected static native int flushStats(long engine);

  /**
   * Retrieve the value of all active stats. Note that this function may block for some time.
   * @return The list of active stats and their values, or empty string of the operation failed
   */
  protected static native String dumpStats();

  /**
   * Provides a configuration template that may be used for building platform
   * filter config chains.
   *
   * @return A template that may be used as a starting point for constructing
   * platform filter configuration.
   */
  public static native String platformFilterTemplate();

  /**
   * Provides a configuration template that may be used for building native
   * filter config chains.
   *
   * @return A template that may be used as a starting point for constructing
   * native filter configuration.
   */
  public static native String nativeFilterTemplate();

  /**
   * Provides a configuration insert that may be used to include an instance
   * of the AlternateProtocolsCacheFilter in the filter chain. Needed only
   * when (experimental) QUIC/H3 support is enabled.
   */
  public static native String altProtocolCacheFilterInsert();

  /**
   * Provides a configuration insert that may be used to enable gzip decompression.
   */
  public static native String gzipConfigInsert();

  /**
   * Provides a configuration insert that may be used to enable brotli decompression.
   */
  public static native String brotliConfigInsert();

  /**
   * Provides a configuration that may be used to enable a persistent DNS cache.
   */
  public static native String persistentDNSCacheConfigInsert();

  /**
   * Provides a template to config the certification validator to be used.
   *
   * @param usePlatform true if the usage of platform validation APIs is desired.
   * @return string, the config template string.
   */
  public static native String certValidationTemplate(boolean usePlatform);

  /**
   * Provides a configuration insert that may be used to enable socket tagging.
   */
  public static native String socketTagConfigInsert();

  /**
   * Register a platform-provided key-value store implementation.
   *
   * @param name,    unique name identifying this key-value store.
   * @param context, context containing logic necessary to invoke the key-value store.
   * @return int,    the resulting status of the operation.
   */
  protected static native int registerKeyValueStore(String name, JvmKeyValueStoreContext context);

  /**
   * Register a string accessor to get strings from the platform.
   *
   * @param accessorName, unique name identifying this accessor.
   * @param context,      context containing logic necessary to invoke the accessor.
   * @return int, the resulting status of the operation.
   */
  protected static native int registerStringAccessor(String accessorName,
                                                     JvmStringAccessorContext context);

  /**
   * Refresh DNS, and drain connections owned by this Engine.
   *
   * @param engine Handle to the engine for which to drain connections.
   */
  protected static native int resetConnectivityState(long engine);

  /**
   * Update the network interface to the preferred network for opening new
   * streams. Note that this state is shared by all engines.
   *
   * @param engine  Handle to the engine whose preferred network will be set.
   * @param network the network to be preferred for new streams.
   * @return The resulting status of the operation.
   */
  protected static native int setPreferredNetwork(long engine, int network);

  /**
   * Update the proxy settings.
   *
   * @param engine Handle to the engine whose proxy settings should be updated.
   * @param host The proxy host.
   * @param port The proxy port.
   * @return The resulting status of the operation.
   */
  protected static native int setProxySettings(long engine, String host, int port);

  /**
   * Mimic a call to AndroidNetworkLibrary#verifyServerCertificates from native code.
   * To be used for testing only.
   *
   * @param certChain The ASN.1 DER encoded bytes for certificates.
   * @param authType The key exchange algorithm name (e.g. RSA).
   * @param host The hostname of the server.
   * @return Android certificate verification result code.
   */
  public static native Object callCertificateVerificationFromNative(byte[][] certChain,
                                                                    byte[] authType, byte[] host);
  /**
   * Mimic a call to AndroidNetworkLibrary#addTestRootCertificate from native code.
   * To be used for testing only.
   *
   * @param rootCert DER encoded bytes of the certificate.
   */
  public static native void callAddTestRootCertificateFromNative(byte[] cert);

  /**
   * Mimic a call to AndroidNetworkLibrary#clearTestRootCertificate from native code.
   * To be used for testing only.
   *
   */
  public static native void callClearTestRootCertificateFromNative();

  /**
   * Takes the various fields from EnvoyConfiguration, and the YAML EnvoyConfiguration generates
   * and compares the YAML generated by the C++ builder to the YAML generated by the Java builder.
   * Fails a release assert if the two are not the same.
   * To be used for testing.
   *
   */
  public static native String compareYaml(
      String yaml, String grpcStatsDomain, boolean adminInterfaceEnabled,
      long connectTimeoutSeconds, long dnsRefreshSeconds, long dnsFailureRefreshSecondsBase,
      long dnsFailureRefreshSecondsMax, long dnsQueryTimeoutSeconds, long dnsMinRefreshSeconds,
      byte[][] dnsPreresolveHostnames, boolean enableDNSCache, long dnsCacheSaveIntervalSeconds,
      boolean enableDrainPostDnsRefresh, boolean enableHttp3, boolean enableGzip,
      boolean enableBrotli, boolean enableSocketTagging, boolean enableHappyEyeballs,
      boolean enableInterfaceBinding, long h2ConnectionKeepaliveIdleIntervalMilliseconds,
      long h2ConnectionKeepaliveTimeoutSeconds, long maxConnectionsPerHost, long statsFlushSeconds,
      long streamIdleTimeoutSeconds, long perTryIdleTimeoutSeconds, String appVersion, String appId,
      boolean trustChainVerification, byte[][] virtualClusters, byte[][] filterChain,
      byte[][] statSinks, boolean enablePlatformCertificatesValidation,
      boolean enableSkipDNSLookupForProxiedRequests);
}
