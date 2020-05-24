package cn.xzzz2020.miaosha.util;


import org.apache.commons.codec.digest.DigestUtils;

public class MD5Util {


    public static String md5(String str){
        return DigestUtils.md5Hex(str);
    }

    private static final String salt = "1a2b3c4d";

    public static String fromPassToDB(String fromPass,String salt){
        String str = "" + salt.charAt(0) + salt.charAt(2) + fromPass+salt.charAt(5)+salt.charAt(4);
        return md5(str);
    }

    public static String inputPassFrom(String inputPass){
        String str = "" + salt.charAt(0) + salt.charAt(2) + inputPass+salt.charAt(5)+salt.charAt(4);
        return md5(str);
    }

    public static String inputPassToDbPass(String input,String saltDB){
        String passFrom = inputPassFrom(input);
        String s = fromPassToDB(passFrom, saltDB);
        return s;
    }



    public static void main(String[] args) {
//        System.out.println(inputPassFrom("a001"));
        System.out.println(inputPassToDbPass("123456","1a2b3c4d"));
    }


}
