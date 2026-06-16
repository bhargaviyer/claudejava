package com.yourpackage.claude;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * ☕ Java Meets Claude | Post 6 of 8 — Prompt Evaluation, Part 1: Methodology
 *
 * Java translation of the prompt evaluation pattern from Anthropic's
 * official course. Original demonstrated in Python — this is the Java port.
 *
 * The framework has four parts:
 *   1. A dataset of test cases
 *   2. A function that runs the prompt against each case
 *   3. A deterministic grader (does the output compile / parse?)
 *   4. A model-based grader (is the output actually good?)
 *
 * The final score for each case is the average of both graders.
 * The final score for the eval is the average across all cases.
 *
 * Prerequisites:
 *   - ANTHROPIC_API_KEY environment variable set
 *   - Maven dependencies:
 *
 *     <dependency>
 *       <groupId>com.anthropic</groupId>
 *       <artifactId>anthropic-java</artifactId>
 *       <version>0.8.0</version>  <!-- verify latest -->
 *     </dependency>
 *     <dependency>
 *       <groupId>com.fasterxml.jackson.core</groupId>
 *       <artifactId>jackson-databind</artifactId>
 *       <version>2.17.0</version>  <!-- verify latest -->
 *     </dependency>
 *
 * Note: This file demonstrates the framework. For production use, split
 * into separate classes (TestCase, Grader, EvalRunner, etc.).
 */
public class PromptEvalExample {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final AnthropicClient CLIENT = AnthropicOkHttpClient.fromEnv();

    // -----------------------------------------------------------------
    // Data models — using Java records for the eval case + result types.
    // -----------------------------------------------------------------

    /** A single test case in the eval dataset. */
    record TestCase(String task, String format, String solutionCriteria) {}

    /** Structured grade returned by the model-based grader. */
    record ModelGrade(List<String> strengths,
                      List<String> weaknesses,
                      String reasoning,
                      int score) {}

    /** Final scored result for one test case. */
    record EvalResult(TestCase testCase,
                      String output,
                      double syntaxScore,
                      int modelScore,
                      String modelReasoning,
                      double finalScore) {}

    // -----------------------------------------------------------------
    // The dataset — hardcoded here for the example. In a real eval,
    // load this from a JSON file or a database.
    // -----------------------------------------------------------------
    private static final List<TestCase> DATASET = List.of(
            new TestCase(
                    "Write a Java method to extract the AWS region from an ARN string",
                    "java",
                    "Handles standard ARN format, returns null or Optional for invalid input, no external dependencies"
            ),
            new TestCase(
                    "Create a JSON configuration for an AWS Lambda function with environment variables for DB connection",
                    "json",
                    "Includes function name, runtime, env variables for DB host, port, name, username, password reference"
            ),
            new TestCase(
                    "Design a regular expression to validate an AWS EC2 instance ID (format: i-[alphanumeric])",
                    "regex",
                    "Matches valid i-XXXX patterns, rejects clearly invalid inputs, properly anchored"
            )
    );

    public static void main(String[] args) {
        double total = 0;
        for (TestCase testCase : DATASET) {
            EvalResult result = runTestCase(testCase);
            total += result.finalScore();

            System.out.println("=".repeat(60));
            System.out.println("Task: " + result.testCase().task());
            System.out.println("Syntax score: " + result.syntaxScore() + "/10");
            System.out.println("Model score:  " + result.modelScore() + "/10");
            System.out.println("Final score:  " + result.finalScore() + "/10");
            System.out.println("Reasoning:    " + result.modelReasoning());
            System.out.println();
        }

        double average = total / DATASET.size();
        System.out.println("=".repeat(60));
        System.out.println("AVERAGE SCORE ACROSS " + DATASET.size() + " CASES: " + average + "/10");
    }

    // -----------------------------------------------------------------
    // Step 1: Run the prompt against a test case.
    // -----------------------------------------------------------------

    /**
     * Sends the test case task to Claude and returns the raw output.
     * Uses temperature 0.0 for reproducibility.
     */
    private static String runPrompt(TestCase testCase) {
        String prompt = """
                Please solve the following task:

                %s

                Respond only with Java, JSON, or a plain Regex.
                Do not add any comments, commentary, or explanation.
                """.formatted(testCase.task());

        Message message = CLIENT.messages().create(
                MessageCreateParams.builder()
                        .model(Model.CLAUDE_HAIKU_4_5)
                        .maxTokens(1024)
                        .temperature(0.0)
                        .addUserMessage(prompt)
                        .build()
        );
        return message.content().get(0).asText().text();
    }

    // -----------------------------------------------------------------
    // Step 2: Deterministic syntax grader.
    // Returns 10 if the output parses/compiles in its declared format,
    // 0 otherwise. Binary signal — no partial credit.
    // -----------------------------------------------------------------

    private static double gradeBySyntax(String output, String format) {
        String trimmed = output.trim();
        return switch (format) {
            case "json" -> validateJson(trimmed) ? 10 : 0;
            case "regex" -> validateRegex(trimmed) ? 10 : 0;
            case "java" -> validateJavaStructure(trimmed) ? 10 : 0;
            default -> 0;
        };
    }

    private static boolean validateJson(String text) {
        try {
            MAPPER.readTree(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean validateRegex(String text) {
        try {
            Pattern.compile(text);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    /**
     * Quick structural check for Java — looks for balanced braces and
     * either a class or method signature. Not a full compile check.
     *
     * For a stricter check, use javax.tools.JavaCompiler to compile
     * the output in-memory. That's more thorough but adds code we
     * don't need for the example.
     */
    private static boolean validateJavaStructure(String text) {
        long openBraces = text.chars().filter(c -> c == '{').count();
        long closeBraces = text.chars().filter(c -> c == '}').count();
        boolean hasJavaKeywords = text.contains("public") || text.contains("class") || text.contains("static");
        return openBraces > 0 && openBraces == closeBraces && hasJavaKeywords;
    }

    // -----------------------------------------------------------------
    // Step 3: Model-based grader.
    // Uses Claude itself as a judge. Returns a structured grade
    // by prefilling the assistant message with "{" and stopping at "}".
    // -----------------------------------------------------------------

    private static ModelGrade gradeByModel(TestCase testCase, String output) {
        String evalPrompt = """
                You are an expert code reviewer. Evaluate the following AI-generated solution.

                Original Task:
                <task>%s</task>

                Solution to Evaluate:
                <solution>%s</solution>

                Criteria:
                <criteria>%s</criteria>

                Respond with a single JSON object with exactly these fields, in this order:
                  "strengths": array of 1-3 strings
                  "weaknesses": array of 1-3 strings
                  "reasoning": a concise explanation
                  "score": integer between 1 and 10

                Respond with JSON only. No explanation outside the JSON.
                """.formatted(testCase.task(), output, testCase.solutionCriteria());

        Message message = CLIENT.messages().create(
                MessageCreateParams.builder()
                        .model(Model.CLAUDE_HAIKU_4_5)
                        .maxTokens(1024)
                        .temperature(0.0)
                        .addUserMessage(evalPrompt)
                        // Prefill the response with "{" so the model continues from there.
                        // Stop sequence "}" closes off the JSON cleanly.
                        .addAssistantMessage("{")
                        .stopSequences(List.of("}"))
                        .build()
        );

        String raw = "{" + message.content().get(0).asText().text() + "}";
        try {
            JsonNode node = MAPPER.readTree(raw);
            return new ModelGrade(
                    List.of(),  // simplified for the example — parse arrays for full impl
                    List.of(),
                    node.get("reasoning").asText(""),
                    node.get("score").asInt(0)
            );
        } catch (Exception e) {
            // If the grader's JSON failed to parse, give a zero with a note.
            return new ModelGrade(List.of(), List.of(), "Grader output unparseable: " + e.getMessage(), 0);
        }
    }

    // -----------------------------------------------------------------
    // Step 4: Run one test case end-to-end.
    // -----------------------------------------------------------------

    private static EvalResult runTestCase(TestCase testCase) {
        String output = runPrompt(testCase);
        double syntaxScore = gradeBySyntax(output, testCase.format());
        ModelGrade modelGrade = gradeByModel(testCase, output);
        double finalScore = (syntaxScore + modelGrade.score()) / 2.0;

        return new EvalResult(
                testCase,
                output,
                syntaxScore,
                modelGrade.score(),
                modelGrade.reasoning(),
                finalScore
        );
    }
}