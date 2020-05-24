package cn.xzzz2020.miaosha.exception;

import cn.xzzz2020.miaosha.result.CodeMsg;

/**
 * 全局的异常
 */
public class GlobalException extends RuntimeException {

    private final CodeMsg cm;

    public GlobalException(CodeMsg cm) {
        super(cm.toString());
        this.cm = cm;
    }

    public CodeMsg getCm() {
        return cm;
    }
}
