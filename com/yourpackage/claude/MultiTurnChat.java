package com.yourpackage.claude;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MultiTurnChat {

    public static void main(String[] args) {

        AnthropicClient client = AnthropicOkHttpClient.fromEnv();
        List<MessageParam> history = new ArrayList<>();
        Scanner scanner = new Scanner(System.in);

        System.out.println("Chat with Claude. Type 'exit' to quit.");

        while (true) {
            System.out.print("You: ");
            String userInput = scanner.nextLine();

            if (userInput.equalsIgnoreCase("exit")) break;

            history.add(
                    MessageParam.builder()
                            .role(MessageParam.Role.USER)
                            .content(userInput)
                            .build()
            );

            MessageCreateParams params = MessageCreateParams.builder()
                    .maxTokens(1024L)
                    .model(Model.CLAUDE_SONNET_4_6)
                    .messages(history)
                    .build();

            Message response = client.messages().create(params);
            String reply = response.content().get(0).asText().text();

            history.add(
                    MessageParam.builder()
                            .role(MessageParam.Role.ASSISTANT)
                            .content(reply)
                            .build()
            );

            System.out.println("Claude: " + reply);
        }

        scanner.close();
    }
}