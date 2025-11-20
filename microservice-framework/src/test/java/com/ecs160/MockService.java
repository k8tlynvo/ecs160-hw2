package com.ecs160;

import com.ecs160.annotations.Microservice;
import com.ecs160.annotations.Endpoint;

@Microservice
public class MockService {

    @Endpoint(url="/greeting")
    public String hello(String input) {
        return "Hello " + input;
    }

    @Endpoint(url="/echo")
    public String add(String input) {
        return input;
    }
}