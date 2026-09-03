package org.example.courseselectionsystem.config;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import org.example.courseselectionsystem.ai.CourseAssistant;
import org.example.courseselectionsystem.ai.tool.AssistantDataTools;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * AI 助手配置（微服务版）
 * 说明：本项目不使用 langchain4j 的 Spring Boot Starter（其面向 Boot 3/jakarta），
 * 而是直接装配 ChatModel / StreamingChatModel / AiServices，兼容 Spring Boot 2.7。
 * ChatMemory 为每个会话记忆最近 20 条消息。
 */
@Configuration
public class AiConfiguration {

    @Value("${langchain4j.open-ai.chat-model.base-url}")
    private String baseUrl;

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.chat-model.model-name}")
    private String modelName;

    @Value("${langchain4j.open-ai.chat-model.temperature:0.7}")
    private double temperature;

    @Value("${langchain4j.open-ai.chat-model.max-tokens:2000}")
    private int maxTokens;

    @Value("${langchain4j.open-ai.chat-model.timeout:60s}")
    private Duration timeout;

    @Value("${langchain4j.open-ai.streaming-chat-model.base-url}")
    private String streamingBaseUrl;

    @Value("${langchain4j.open-ai.streaming-chat-model.api-key}")
    private String streamingApiKey;

    @Value("${langchain4j.open-ai.streaming-chat-model.model-name}")
    private String streamingModelName;

    @Value("${langchain4j.open-ai.streaming-chat-model.temperature:0.7}")
    private double streamingTemperature;

    @Value("${langchain4j.open-ai.streaming-chat-model.max-tokens:2000}")
    private int streamingMaxTokens;

    @Value("${langchain4j.open-ai.streaming-chat-model.timeout:60s}")
    private Duration streamingTimeout;

    @Bean
    public ChatModel chatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(timeout)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    public StreamingChatModel streamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(streamingBaseUrl)
                .apiKey(streamingApiKey)
                .modelName(streamingModelName)
                .temperature(streamingTemperature)
                .maxTokens(streamingMaxTokens)
                .timeout(streamingTimeout)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean("sseExecutor")
    public Executor sseExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("ai-sse-");
        executor.initialize();
        return executor;
    }

    @Bean
    public CourseAssistant courseAssistant(
            ChatModel chatModel,
            StreamingChatModel streamingChatModel,
            AssistantDataTools assistantDataTools) {
        return AiServices.builder(CourseAssistant.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .maxMessages(20)
                        .build())
                .tools(assistantDataTools)
                .build();
    }
}
