package com.claudeexamples;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;

/**
 * ☕ Java Meets Claude | Post 4 of 8 — System Prompts
 *
 * Demonstrates the difference between calling Claude with and without
 * a system prompt, using a real-world Java retry method as the example.
 *
 * Prerequisites:
 *   - ANTHROPIC_API_KEY set as an environment variable
 *   - Maven dependency:
 *
 *     <dependency>
 *       <groupId>com.anthropic</groupId>
 *       <artifactId>anthropic-java</artifactId>
 *       <version>0.8.0</version>  <!-- verify latest at: https://github.com/anthropics/anthropic-sdk-java -->
 *     </dependency>
 */
public class SystemPromptsExample {

    // -----------------------------------------------------------------
    // System prompt — defines WHO Claude is for every message
    // that follows in this conversation.
    // -----------------------------------------------------------------
    private static final String JAVA_ENGINEER_SYSTEM_PROMPT = """
            You are a senior Java engineer with 10+ years of production experience.
            
            Rules you must always follow:
            - Always write Java. Never Python, JavaScript, or pseudocode.
            - Use typed exceptions. Never swallow exceptions silently.
            - Follow standard Java naming conventions (camelCase methods, PascalCase classes).
            - Never use raw types or unchecked casts.
            - Use Optional instead of returning null.
            - Keep methods focused — one responsibility per method.
            - Add a brief Javadoc comment to every public method.
            
            Output format:
            - Return only the Java code, no explanation unless asked.
            - Include necessary imports at the top.
            """;

    // -----------------------------------------------------------------
    // The task — same question sent in both examples
    // -----------------------------------------------------------------
    private static final String TASK =
            "Write a method to retry a failed HTTP call with exponential backoff. " +
            "Max 3 retries. Throw an exception if all retries fail.";

    public static void main(String[] args) {
        AnthropicClient client = AnthropicOkHttpClient.fromEnv();

        System.out.println("=".repeat(60));
        System.out.println("EXAMPLE 1: Without a system prompt");
        System.out.println("=".repeat(60));
        String withoutSystemPrompt = callWithoutSystemPrompt(client);
        System.out.println(withoutSystemPrompt);

        System.out.println("\n" + "=".repeat(60));
        System.out.println("EXAMPLE 2: With a system prompt");
        System.out.println("=".repeat(60));
        String withSystemPrompt = callWithSystemPrompt(client);
        System.out.println(withSystemPrompt);
    }

    /**
     * Calls Claude with no system prompt.
     * Claude will make its own assumptions about language, style,
     * error handling, and output format.
     */
    private static String callWithoutSystemPrompt(AnthropicClient client) {
        Message message = client.messages().create(
                MessageCreateParams.builder()
                        .model(Model.CLAUDE_HAIKU_4_5)
                        .maxTokens(1024)
                        .addUserMessageText(TASK)
                        .build()
        );
        return message.content().get(0).asText().text();
    }

    /**
     * Calls Claude with a system prompt that establishes it as a
     * senior Java engineer with explicit coding standards.
     *
     * The system prompt persists for the entire conversation —
     * you define the rules once, every response honours them.
     */
    private static String callWithSystemPrompt(AnthropicClient client) {
        Message message = client.messages().create(
                MessageCreateParams.builder()
                        .model(Model.CLAUDE_HAIKU_4_5)
                        .maxTokens(1024)
                        .system(JAVA_ENGINEER_SYSTEM_PROMPT)
                        .addUserMessageText(TASK)
                        .build()
        );
        return message.content().get(0).asText().text();
    }
}
