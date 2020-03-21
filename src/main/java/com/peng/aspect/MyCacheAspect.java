package com.peng.aspect;


import com.peng.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Slf4j
@Aspect
@Component
public class MyCacheAspect {
    @Autowired
    private RedisUtil redisUtil;

    @Around("execution(public * com.peng.service.Impl..*(..)) && @annotation(myCache)")
    public Object around(ProceedingJoinPoint jp, MyCache myCache) throws Throwable {
        long startTime = System.currentTimeMillis();
        //获取Redis中的key
        Signature signature = jp.getSignature();
        String methodName = signature.getName();
        String className = signature.getDeclaringTypeName();
        StringBuffer sbKey = new StringBuffer();
        sbKey.append(className);
        sbKey.append(".");
        sbKey.append(methodName);
        Object[] args = jp.getArgs();//方法参数值
        for (Object object:args) {
            sbKey.append("-");
            sbKey.append(object);
        }
        String key=sbKey.toString();
        //如果有缓存直接返回，没有正常执行并写入缓存
        try {
            if (redisUtil.hasKey(key)) {
//                System.out.println(methodName+"-"+"找到缓存了！");
                return redisUtil.get(key);
            } else {
//                System.out.println(methodName+"----------------没有缓存！");
                Object result = jp.proceed(args);
                redisUtil.set(key, result, 60 * 60);
                return result;
            }
        } catch (Throwable t){
            log.error(t.toString());
            return null;
        } finally {
            System.out.print(methodName+"-"+"方法执行时间：");
            System.out.println(System.currentTimeMillis()-startTime);
        }
    }
}
