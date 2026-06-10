package com.codingshuttle.distributed_lovable.workspace_service.service;

import com.codingshuttle.distributed_lovable.workspace_service.dto.project.DeployResponse;
import jakarta.annotation.Nullable;

public interface DeploymentService {

    DeployResponse deploy(Long projectId);
}
