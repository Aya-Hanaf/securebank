package com.securebank.securebank.mapper;

import com.securebank.securebank.dto.response.AccountResponse;
import com.securebank.securebank.entity.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    @Mapping(target = "userId", source = "user.id")
    AccountResponse toResponse(Account account);

    List<AccountResponse> toResponseList(List<Account> accounts);
}
