package com.yourpackage.claude;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;

/**
 * ☕ Java Meets Claude | Post 5 of 8 — Temperature
 *
 * Demonstrates how the temperature parameter affects Claude's output.
 * Runs the SAME prompt twice at temperature 0.0 (deterministic) and
 * twice at temperature 1.0 (variable) so you can see the difference
 * yourself.
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
 *
 * Note: confirm the exact setter name (.temperature(double)) matches
 * the SDK version you're on before running.
 */
public class Temperature{

    // Same system prompt across all calls — only temperature changes.
    private static final String SYSTEM_PROMPT = "You are a senior Java engineer.Always write production-ready Java. No Python, no pseudocode.Return only the Java code, no explanation.";

    // Same task across all calls.
    private static final String TASK =
            "Write a Java method to validate an email address.";

    public static void main(String[] args) {
        AnthropicClient client = AnthropicOkHttpClient.fromEnv();

        // -----------------------------------------------------------------
        // Temperature 0.0 — deterministic.
        // Run the same prompt twice. Outputs should be nearly identical.
        // -----------------------------------------------------------------
        System.out.println("=".repeat(60));
        System.out.println("TEMPERATURE 0.0 — Run 1");
        System.out.println("=".repeat(60));
        System.out.println(callClaude(client, 0.0));

        System.out.println("\n" + "=".repeat(60));
        System.out.println("TEMPERATURE 0.0 — Run 2 (should match Run 1)");
        System.out.println("=".repeat(60));
        System.out.println(callClaude(client, 0.0));

        // -----------------------------------------------------------------
        // Temperature 1.0 — variable.
        // Same prompt, run twice. Outputs will differ in approach,
        // style, naming, or structure.
        // -----------------------------------------------------------------
        System.out.println("\n" + "=".repeat(60));
        System.out.println("TEMPERATURE 1.0 — Run 1");
        System.out.println("=".repeat(60));
        System.out.println(callClaude(client, 1.0));

        System.out.println("\n" + "=".repeat(60));
        System.out.println("TEMPERATURE 1.0 — Run 2 (likely different)");
        System.out.println("=".repeat(60));
        System.out.println(callClaude(client, 1.0));
    }

    /**
     * Calls Claude with the given temperature.
     * Everything else (model, system prompt, user message) is identical.
     *
     * @param client      the Anthropic API client
     * @param temperature 0.0 = deterministic, 1.0 = maximum variability
     * @return the text response from Claude
     */
    private static String callClaude(AnthropicClient client, double temperature) {
        Message message = client.messages().create(
                MessageCreateParams.builder()
                        .model(Model.CLAUDE_HAIKU_4_5)
                        .maxTokens(1024)
                        .temperature(temperature)
                        .system(SYSTEM_PROMPT)
                        .addUserMessage(TASK)
                        .build()
        );
        return message.content().get(0).asText().text();
    }
}
