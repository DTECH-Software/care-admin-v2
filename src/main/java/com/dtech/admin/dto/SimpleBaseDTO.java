/**
 * User: Himal_J
 * Date: 2/22/2025
 * Time: 6:32 PM
 * <p>
 */

package com.dtech.admin.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimpleBaseDTO {
    private String code;
    private String description;

    @JsonCreator
    public static SimpleBaseDTO fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return new SimpleBaseDTO(node.asText(), null);
        }
        if (node.isObject()) {
            JsonNode codeNode = node.get("code");
            JsonNode descriptionNode = node.get("description");
            return new SimpleBaseDTO(
                    codeNode != null && !codeNode.isNull() ? codeNode.asText() : null,
                    descriptionNode != null && !descriptionNode.isNull() ? descriptionNode.asText() : null
            );
        }
        throw new IllegalArgumentException("Invalid SimpleBaseDTO payload");
    }
}
