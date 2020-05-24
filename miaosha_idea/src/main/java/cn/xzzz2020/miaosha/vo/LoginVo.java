package cn.xzzz2020.miaosha.vo;

import cn.xzzz2020.miaosha.validator.IsMobile;
import lombok.Data;
import lombok.NonNull;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;

/**
 * JSR303 参数校验
 */
@Data
public class LoginVo {

    @NotNull
    @IsMobile
    private String mobile;

    @NotNull
    @Length(max = 32)
    private String password;
}
