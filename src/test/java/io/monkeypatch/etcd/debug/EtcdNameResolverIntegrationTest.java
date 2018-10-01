package io.monkeypatch.etcd.debug;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.ibm.etcd.client.DebugEtcdClient;
import com.ibm.etcd.client.EtcdClient;
import com.palantir.docker.compose.DockerComposeRule;
import io.grpc.Deadline;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import static com.ibm.etcd.client.KeyUtils.bs;
import static org.junit.Assert.assertEquals;

public class EtcdNameResolverIntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(EtcdNameResolverIntegrationTest.class);
    private static final long DEFAULT_TIMEOUT_MS = 2_000L;
    private static final long DEADLINE_TIMEOUT_MS = 10_000L;

    private static String etcdHost1;

    private static String dockerComposePath;
    static {
        try {
            dockerComposePath = new java.io.File(Resources.getResource("docker-compose.yml").toURI()).getAbsolutePath();
        } catch (URISyntaxException e) { e.printStackTrace(); }
    }

    private static DockerComposeRule docker;
    private static EtcdClient client;

    @ClassRule
    public static TestRule chain = RuleChain
            .outerRule(docker = DockerComposeRule.builder()
                    .file(dockerComposePath)
                    .pullOnStartup(true)
                    .build())
            .around(new ExternalResource() {
                @Override
                protected void before() {
                    etcdHost1 = docker.containers()
                            .container("etcd")
                            .port(2379)
                            .inFormat("http://$HOST:$EXTERNAL_PORT");
                }
            });

    @BeforeClass
    public static void setup() {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        client = DebugEtcdClient.forEndpoints(etcdHost1)
                .withDefaultTimeout(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .withPlainText()
                .build();
    }

    @Test
    public void testEtcdClient() throws InterruptedException {
        // Channel exits idle mode and request succeeds
        client.getKvClient()
                .put(bs("some-key"), bs("some-value"))
                .timeout(DEFAULT_TIMEOUT_MS)
                .backoffRetry()
                .deadline(Deadline.after(DEADLINE_TIMEOUT_MS, TimeUnit.MILLISECONDS))
                .sync();

        // Wait for 10sec for the channel to go idle as specified via idleTimeout
        LOG.info("Waiting");
        Thread.sleep(10_000);

        // Channel exits idle mode again
        assertEquals("some-value",
                client.getKvClient()
                        .get(bs("some-key"))
                        .timeout(DEFAULT_TIMEOUT_MS)
                        .backoffRetry()
                        .deadline(Deadline.after(DEADLINE_TIMEOUT_MS, TimeUnit.MILLISECONDS))
                        .sync()
                        .getKvs(0)
                        .getValue()
                        .toStringUtf8());
    }
}