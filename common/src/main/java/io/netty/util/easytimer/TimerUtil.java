package io.netty.util.easytimer;

import io.netty.util.Timeout;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * <一句话功能简述>自己实现的定时器工具类 <功能详细描述>
 * 
 * @author 姓名: along
 * @version [版本号, 2014-10-10]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */
public class TimerUtil {
    public static Map<String,Timeout> timeoutMap=new HashMap<String, Timeout>();
}
