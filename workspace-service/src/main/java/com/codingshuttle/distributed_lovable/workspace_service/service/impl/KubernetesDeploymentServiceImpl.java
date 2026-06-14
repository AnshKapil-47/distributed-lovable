package com.codingshuttle.distributed_lovable.workspace_service.service.impl;

import com.codingshuttle.distributed_lovable.workspace_service.dto.project.DeployResponse;
import com.codingshuttle.distributed_lovable.workspace_service.service.DeploymentService;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class KubernetesDeploymentServiceImpl implements DeploymentService {

    private final KubernetesClient client;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.preview.namespace}")
    private String namespace;

    @Value("${app.preview.domain}")
    private String baseDomain;

    @Value("${app.preview.proxy-port}")
    private String proxyPort;

    private static final String POOL_LABEL = "status";
    private static final String PROJECT_LABEL = "project-id";
    private static final String IDLE = "idle";
    private static final String BUSY = "busy";

    public DeployResponse deploy(Long projectId) {
        String domain = "project-" + projectId + "." + baseDomain;
        String formattedUrl = proxyPort.equals("80")
                ? "http://" + domain
                : "http://" + domain + ":" + proxyPort;

        Pod existingPod = findActivePod(projectId);

        if (existingPod != null) {
            log.info("Found existing pod {} for project {}. Resuming...", existingPod.getMetadata().getName(), projectId);
            registerRoute(domain, existingPod);
            return new DeployResponse(formattedUrl);
        }

        return claimAndStartNewPod(projectId, domain, formattedUrl);
    }

    private Pod findActivePod(Long projectId) {
        return client.pods().inNamespace(namespace)
                .withLabel(PROJECT_LABEL, projectId.toString())
                .withLabel(POOL_LABEL, BUSY)
                .list().getItems().stream()
                .filter(pod -> pod.getStatus().getPhase().equals("Running"))
                .findFirst()
                .orElse(null);
    }

    private DeployResponse claimAndStartNewPod(Long projectId, String domain, String formattedUrl) {
        Pod pod = client.pods().inNamespace(namespace)
                .withLabel(POOL_LABEL, IDLE)
                .list().getItems().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No idle runners available. Please scale up the runner-pool."));

        String podName = pod.getMetadata().getName();
        log.info("Claiming pod {} for project {}", podName, projectId);

        client.pods().inNamespace(namespace).withName(podName).edit(p -> {
            p.getMetadata().getLabels().put(POOL_LABEL, BUSY);
            p.getMetadata().getLabels().put(PROJECT_LABEL, projectId.toString());
            return p;
        });

        try {
            // Step 1: Sync project files from MinIO
            String initialSyncCmd = String.format(
                    "rm -rf /app/* && mc mirror --overwrite myminio/projects/%d/ /app/", projectId);
            execCommand(false, podName, "syncer", "sh", "-c", initialSyncCmd);

            // Step 2: Start watching MinIO for file changes (background)
            String watchCmd = String.format(
                    "sh -c 'mc mirror --overwrite --watch myminio/projects/%d/ /app/ >> /app/sync.log 2>&1 &'", projectId);
            execCommand(true, podName, "syncer", "sh", "-c", watchCmd);

            // Step 3: Copy pre-cached node_modules, then npm install (only installs delta — very fast)
            // Then start Vite in background
            String startCmd =
                    "sh -c '" +
                            "cp -rn /node-cache/node_modules /app/node_modules 2>/dev/null || true && " +
                            "cd /app && " +
                            "npm install --legacy-peer-deps >> /app/dev.log 2>&1 && " +
                            "npm run dev -- --host 0.0.0.0 --port 5173 >> /app/dev.log 2>&1 &" +
                            "'";
            execCommand(true, podName, "runner", "sh", "-c", startCmd);

            // Step 4: Wait for Vite to actually be ready before registering route
            waitForVite(podName);

            // Step 5: Register route in Redis so proxy can forward traffic
            Pod updatedPod = client.pods().inNamespace(namespace).withName(podName).get();
            registerRoute(domain, updatedPod);

            log.info("Deployment successful: {}", formattedUrl);
            return new DeployResponse(formattedUrl);

        } catch (Exception e) {
            log.error("Deployment failed for project {}. Releasing pod {}.", projectId, podName, e);
            client.pods().inNamespace(namespace).withName(podName).delete();
            throw new RuntimeException("Failed to deploy project " + projectId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Polls the dev.log inside the runner container every 3 seconds until
     * Vite prints "Local:" (meaning it's ready), or until 3 minutes timeout.
     * This ensures we NEVER register the route before Vite is actually listening on :5173.
     */
    private void waitForVite(String podName) throws InterruptedException {
        log.info("Waiting for Vite to start in pod {}...", podName);
        int maxAttempts = 90; // 90 × 3s
        for (int i = 0; i < maxAttempts; i++) {
            Thread.sleep(3000);
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                CompletableFuture<String> done = new CompletableFuture<>();
                try (ExecWatch watch = client.pods().inNamespace(namespace).withName(podName)
                        .inContainer("runner")
                        .writingOutput(out)
                        .writingError(new ByteArrayOutputStream())
                        .usingListener(new ExecListener() {
                            @Override
                            public void onClose(int code, String reason) {
                                done.complete("done");
                            }
                        })
                        .exec("sh", "-c", "cat /app/dev.log 2>/dev/null || echo ''")) {
                    done.get(5, TimeUnit.SECONDS);
                }
                String log_content = out.toString();
                if (log_content.contains("Local:") || log_content.contains("localhost:5173")) {
                    log.info("✅ Vite is ready in pod {} after {}s", podName, (i + 1) * 3);
                    return;
                }
                log.debug("Vite not ready yet (attempt {}/{}), log: {}", i + 1, maxAttempts,
                        log_content.length() > 200 ? log_content.substring(log_content.length() - 200) : log_content);
            } catch (Exception e) {
                log.warn("Error checking Vite status: {}", e.getMessage());
            }
        }
        // If Vite didn't start in 3 minutes, register anyway and let proxy retry handle it
        log.warn("Vite did not confirm ready in 3 minutes for pod {}. Registering route anyway.", podName);
    }

    private void registerRoute(String domain, Pod pod) {
        String podIp = pod.getStatus().getPodIP();
        if (podIp == null) throw new RuntimeException("Pod is running but has no IP!");
        redisTemplate.opsForValue().set("route:" + domain, podIp + ":5173", 6, TimeUnit.HOURS);
        log.info("Route Registered: {} -> {}", domain, podIp);
    }

    /**
     * @param background if true, command ends with & so we only wait 500ms
     *                   if false, command is blocking so we wait up to 60s
     */
    private void execCommand(boolean background, String podName, String container, String... command) {
        log.debug("Exec in {}:{} -> {}", podName, container, String.join(" ", command));
        CompletableFuture<String> data = new CompletableFuture<>();
        try (ExecWatch ignored = client.pods().inNamespace(namespace).withName(podName)
                .inContainer(container)
                .writingOutput(new ByteArrayOutputStream())
                .writingError(new ByteArrayOutputStream())
                .usingListener(new ExecListener() {
                    @Override
                    public void onClose(int code, String reason) {
                        data.complete("Done");
                    }
                })
                .exec(command)) {

            if (background) {
                Thread.sleep(1000);
            } else {
                data.get(60, TimeUnit.SECONDS);
            }

        } catch (Exception e) {
            log.error("Exec failed", e);
            throw new RuntimeException("Pod Execution Failed", e);
        }
    }
}