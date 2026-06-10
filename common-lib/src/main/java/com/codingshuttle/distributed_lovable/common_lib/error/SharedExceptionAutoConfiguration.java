package com.codingshuttle.distributed_lovable.common_lib.error;

import org.springframework.context.annotation.Bean;

public class SharedExceptionAutoConfiguration {

    @Bean
    public GlobalExceptionalHandler globalExceptionalHandler(){
        return new GlobalExceptionalHandler();
    }
}
