package cn.xzzz2020.miaosha.web.access;

import cn.xzzz2020.miaosha.domain.MiaoshaUser;
import cn.xzzz2020.miaosha.redis.RedisService;
import cn.xzzz2020.miaosha.redis.key.prefix.AccessKey;
import cn.xzzz2020.miaosha.result.CodeMsg;
import cn.xzzz2020.miaosha.result.Result;
import cn.xzzz2020.miaosha.web.service.MiaoshaUserService;
import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@Service
public class AccessInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    MiaoshaUserService miaoshaUserService;

    @Autowired
    RedisService redisService;

    /**
     * 拦截器
     * 从请求中的Cookie获取用户信息
     *
     * @return
     * @throws Exception
     */

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            AccessLimit limit = handlerMethod.getMethodAnnotation(AccessLimit.class);
            if (limit == null) {
                return super.preHandle(request, response, handler);
            }

            MiaoshaUser user = getMiaoshaUser(request, response);
            int seconds = limit.seconds();
            int maxCount = limit.maxCount();
            boolean needLogin = limit.needLogin();
            String key = request.getRequestURI();
            if (needLogin) {
                if (user == null) {
                    render(response, CodeMsg.SESSION_ERROR);
                    return false;
                }
                key += "_" + user.getId();
            }

            //查询访问的次数
            AccessKey ACCESS = AccessKey.accessTime(seconds);
            Integer relCount = redisService.get(ACCESS, ":" + key, Integer.class);
            if (relCount == null) {//第一次访问
                redisService.set(ACCESS, ":" + key, 1);
            } else if (relCount < maxCount) {
                redisService.incr(ACCESS, ":" + key);
            } else {
                render(response, CodeMsg.ACCESS_LIMIT);
                return false;
            }
        }
        return super.preHandle(request, response, handler);
    }

    private void render(HttpServletResponse response, CodeMsg sessionError) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        OutputStream outputStream = response.getOutputStream();
        String s = JSON.toJSONString(Result.error(sessionError));
        outputStream.write(s.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();
    }

    private MiaoshaUser getMiaoshaUser(HttpServletRequest request, HttpServletResponse response) {
        String paramToken = request.getParameter(MiaoshaUserService.COOKIE_NAME_TOKEN);
        String cookieToken = getCookieValue(request);
        if (StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)) {
            return null;
        }
        String token = StringUtils.isEmpty(paramToken) ? cookieToken : paramToken;
        return miaoshaUserService.getByToken(response, token);
    }

    private String getCookieValue(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length <= 0) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(MiaoshaUserService.COOKIE_NAME_TOKEN)) {
                return cookie.getValue();
            }
        }
        return null;
    }

}
