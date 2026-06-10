package com.codingshuttle.distributed_lovable.common_lib.event;

import lombok.Builder;

@Builder
public record FileStoreRequestEvent(
        Long projectId,
        String sagaId,
        String filePath,
        String content,
        Long userId
) {
}
