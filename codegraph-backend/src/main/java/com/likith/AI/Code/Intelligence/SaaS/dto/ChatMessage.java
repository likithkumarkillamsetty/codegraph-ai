package com.likith.AI.Code.Intelligence.SaaS.dto;

/**
 * Represents a single message in a conversation.
 * @param role The role of the message sender ("user" or "assistant")
 * @param content The content of the message
 */
public record ChatMessage(String role, String content) {
}

