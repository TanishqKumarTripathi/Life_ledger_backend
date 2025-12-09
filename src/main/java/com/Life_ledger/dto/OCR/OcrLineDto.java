package com.Life_ledger.dto.OCR;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OcrLineDto {

    @JsonProperty("LineText")
    private String text;

    @JsonProperty("MinTop")
    private int top;

    @JsonProperty("MaxHeight")
    private int height;
}
