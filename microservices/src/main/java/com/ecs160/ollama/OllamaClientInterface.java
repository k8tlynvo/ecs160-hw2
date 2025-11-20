package com.ecs160.ollama;

// Ollama client interface with generate method
// using an interface to easily mock ollama responses for testing
public interface OllamaClientInterface {

    String generate(String model, String prompt) throws Exception;
    
}