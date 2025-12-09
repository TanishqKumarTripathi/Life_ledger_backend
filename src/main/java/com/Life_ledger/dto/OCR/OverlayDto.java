package com.Life_ledger.dto.OCR;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Data
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverlayDto {
    @JsonProperty("Lines")
    private List<OcrLineDto> Lines;
}
