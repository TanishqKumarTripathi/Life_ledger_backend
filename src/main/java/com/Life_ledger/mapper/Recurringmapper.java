package com.Life_ledger.mapper;

import org.springframework.stereotype.Component;

import com.Life_ledger.dto.recurring.RecurringResponseDto;
import com.Life_ledger.entity.RecurringPattern;

@Component
public class Recurringmapper {
    public RecurringResponseDto toDto(RecurringPattern rp) {
        return RecurringResponseDto.builder()
                .id(rp.getId())
                .merchant(rp.getMerchant())
                .amount(rp.getAmount())
                .frequency(rp.getFrequency())
                .nextDueDate(rp.getNextDueDate())
                .build();
    }

}
