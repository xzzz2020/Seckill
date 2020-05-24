package cn.xzzz2020.miaosha.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidatorUtil {

    public static boolean isMobile(String str){
        Pattern mobile_pattern = Pattern.compile("1\\d{10}");
        Matcher m = mobile_pattern.matcher(str);
        return m.matches();
    }


}
