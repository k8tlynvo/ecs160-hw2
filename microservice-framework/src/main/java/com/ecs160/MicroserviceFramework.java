package com.ecs160;

import com.ecs160.annotations.Microservice;
import com.ecs160.annotations.Endpoint;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.ServerSocket;
import java.net.Socket;

public class MicroserviceFramework {
    
    private Map<String, EndpointHandler> urlToEndpoint = new HashMap<String, EndpointHandler>();
    
    // internal class for handling endpoints
    private static class EndpointHandler {
        Object service;
        Method method;
        
        public EndpointHandler(Object service, Method method) {
            this.service = service;
            this.method = method;
        }

        public String invoke(String input) throws Exception {
            return (String) method.invoke(service, input);
        }
    }

    // launch server
    public boolean launch(int port) {
        try {
            // load all classes in the project
            ClassLoaderHelper helper = new ClassLoaderHelper();
            List<Class<?>> allClasses = helper.listClassesInAllJarsInOwnDirectory();
            
            for (Class<?> clazz : allClasses) {
                // check for @Microservice annotation
                if (clazz.isAnnotationPresent(Microservice.class)) {
                    try {
                        // create service instance 
                        Object service = clazz.getDeclaredConstructor().newInstance();
                        
                        // check for @Endpoint annotations 
                        for (Method method : clazz.getDeclaredMethods()) {
                            if (method.isAnnotationPresent(Endpoint.class)) {
                                Endpoint endpoint = method.getAnnotation(Endpoint.class);

                                // check if endpoint has correct signature (String methodName(string input))
                                if (method.getReturnType() != String.class || method.getParameterCount() != 1|| method.getParameterTypes()[0] != String.class) {
                                    throw new Exception("Method " + method.getName() + " must have signature String methodName(String input)");
                                }
                                
                                // add endpoint to map 
                                urlToEndpoint.put(endpoint.url(), new EndpointHandler(service, method));
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to initiate service: " + clazz.getName() + ": " + e.getMessage());
                    }
                }
            }

            // start server
            ServerSocket server = new ServerSocket(port);
            System.out.println("Listening on port: " + port);

            while (true) {
                // acccept requests
                try (
                    Socket client = server.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    OutputStream out = client.getOutputStream()) {
                    
                    // read request line 
                    String req = in.readLine();
                    if (req == null) {
                        continue;
                    }

                    // parse request
                    String[] req_parts = req.split(" ");
                    if (req_parts.length < 2) {
                        sendResponse(out, 400, "Bad Request", "Bad Request");
                        continue;
                    }

                    // only allow POST requests
                    String method = req_parts[0];
                    if (!method.equalsIgnoreCase("POST")) {
                        sendResponse(out, 405, "Method Not Allowed", "Only POST is allowed");
                        continue;
                    }

                    // read body
                    String path = req_parts[1];
                    int contentLength = 0;
                    String line;
                    while (!(line = in.readLine()).isEmpty()) {
                        if (line.startsWith("Content-Length:")) {
                            contentLength = Integer.parseInt(line.substring("Content-Length:".length()).trim());
                        }
                    }
                    String body = "";
                    if (contentLength > 0) {
                        char[] buffer = new char[contentLength];
                        in.read(buffer);
                        body = new String(buffer);
                    }

                    // look up route
                    EndpointHandler handler = urlToEndpoint.get(path);
                    if (handler == null) {
                        sendResponse(out, 404, "Not Found", "Not Found");
                        continue;
                    }

                    // call endpoint
                    String result;
                    try {
                        result = handler.invoke(body);
                        // result = handler.invoke(queryParam);
                    } catch (Exception e) {
                        sendResponse(out, 500, "Internal Server Error", "Internal Server Error: " + e.toString());
                        continue;
                    }

                    // send success response 
                    sendResponse(out, 200, "OK", result);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } 
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // check if endpoint is a part of service
    public boolean hasEndpoint(String url) {

        return urlToEndpoint.containsKey(url);
    }
    
    // invoke endpoints
    public String invoke(String url, String input) throws Exception {
        EndpointHandler handler = urlToEndpoint.get(url);
        if (handler == null) {
            throw new Exception("No such endpoint: " + url);
        }

        return handler.invoke(input);
    }

    // send respose to output stream in http format
    private void sendResponse(OutputStream out, int statusCode, String statusText, String body) throws Exception {
        StringBuilder response = new StringBuilder();
        response.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusText).append("\r\n");
        response.append("Content-Type: text/plain; charset=UTF-8\r\n");
        response.append("Content-Length: ").append(body.length()).append("\r\n");
        response.append("\r\n");
        response.append(body);
        
        out.write(response.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    // FOR TESTING: has the same logic as annotation checking in launch method
    public void scanMicroservice(Object service) throws Exception {
        // check if service is a microservice
        Class<?> clazz = service.getClass();
        if (!clazz.isAnnotationPresent(Microservice.class)) {
            throw new Exception("Class " + clazz.getName() + " is not a microservice");
        }

        // check for @Endpoint annotations 
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Endpoint.class)) {
                Endpoint endpoint = method.getAnnotation(Endpoint.class);

                // check if endpoint has correct signature (String methodName(string input))
                if (method.getReturnType() != String.class || method.getParameterCount() != 1|| method.getParameterTypes()[0] != String.class) {
                    throw new Exception("Method " + method.getName() + " must have signature String methodName(String input)");
                }
                
                // add endpoint to map 
                urlToEndpoint.put(endpoint.url(), new EndpointHandler(service, method));
            }
        }
    }
} 