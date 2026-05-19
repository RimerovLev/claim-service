package com.claims.mvp.user.dto.request;

import com.claims.mvp.user.model.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

public record ChangeRoleRequest (@NotNull Role role){
}
