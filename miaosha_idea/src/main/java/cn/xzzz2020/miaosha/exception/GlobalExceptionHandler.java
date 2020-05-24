package cn.xzzz2020.miaosha.exception;


import cn.xzzz2020.miaosha.result.CodeMsg;
import cn.xzzz2020.miaosha.result.Result;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 全局异常处理
 */
@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {

    /**
     * 异常处理的方式
     * @param request
     * @param e
     * @return
     */
    @ExceptionHandler(value = Exception.class)
    public Result<String>  exceptionHandler(HttpServletRequest request,Exception e){
        e.printStackTrace();
        if (e instanceof BindException){
            BindException bd = (BindException) e;
            List<ObjectError> allErrors = bd.getAllErrors();

            String msg = allErrors.get(0).getDefaultMessage();
            return Result.error(CodeMsg.BIND_ERROR.fillArgs(msg));
        }else if (e instanceof GlobalException){
            GlobalException ge = (GlobalException) e;
            return Result.error(ge.getCm());
        }
        else {
            return Result.error(CodeMsg.SERVER_ERROR);
        }

    }

}
