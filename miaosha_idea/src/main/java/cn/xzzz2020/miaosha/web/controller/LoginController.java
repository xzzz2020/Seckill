package cn.xzzz2020.miaosha.web.controller;


import cn.xzzz2020.miaosha.result.Result;
import cn.xzzz2020.miaosha.web.service.MiaoshaUserService;
import cn.xzzz2020.miaosha.vo.LoginVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;


@RequestMapping("/login")
@Controller
public class LoginController {

    @Autowired
    private MiaoshaUserService miaoshaUserService;

    @RequestMapping("/to_login")
    public String toLogin() {
        return "login";
    }


    @ResponseBody
    @RequestMapping("/do_login")
    public Result<String> doLogin(HttpServletResponse response,@Valid LoginVo loginVo) {
        String token = miaoshaUserService.login(response, loginVo);
        return Result.success(token);
    }

}
