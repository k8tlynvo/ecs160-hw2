package com.ecs160.ollama;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.response.OllamaResult;
import io.github.ollama4j.utils.Options;
import io.github.ollama4j.utils.OptionsBuilder;

// OllamaClient class with ollama API client and generate method wrapper
public class OllamaClient implements OllamaClientInterface {
    private final OllamaAPI ollama;

    public OllamaClient(String url) {
        this.ollama = new OllamaAPI(url);
    }

    public String generate(String model, String prompt) throws Exception {
        StringBuilder fullResponse = new StringBuilder();
        Options options = new OptionsBuilder()
                .setTemperature(0.7f)
                .build();
        OllamaResult result = ollama.generate(model, prompt, false, options, chunk -> {
                fullResponse.append(chunk);
            });
        return fullResponse.toString();
    }
}
