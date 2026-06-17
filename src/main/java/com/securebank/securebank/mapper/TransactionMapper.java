package com.securebank.securebank.mapper;

import com.securebank.securebank.dto.response.TransactionResponse;
import com.securebank.securebank.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "accountId",     source = "account.id")
    @Mapping(target = "accountNumber", source = "account.accountNumber")
    TransactionResponse toResponse(Transaction transaction);

    List<TransactionResponse> toResponseList(List<Transaction> transactions);
}
