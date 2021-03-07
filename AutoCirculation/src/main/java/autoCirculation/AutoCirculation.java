/*
该功能主要用来判断任务是否超过设定的超时时间,从而决定是否把任务发送给下一优先级的资源
要求等待时间只计算工作日时间
工作日时间为周一到周五,9:00--18:00
 */
package autoCirculation;


import org.apache.commons.lang3.time.DateUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class AutoCirculation {
    private static SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
    private static SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws ParseException {
        TimeZone targetTimeZone = TimeZone.getTimeZone("GMT+8");
        TimeZone systemTimeZone = TimeZone.getDefault();
        long offSet = systemTimeZone.getRawOffset() - targetTimeZone.getRawOffset();

        Date currentTime = new Date(System.currentTimeMillis() - offSet);

        Date taskStartTime = sdf2.parse("2021-03-05 17:02:00");
        taskStartTime = new Date(taskStartTime.getTime() - offSet);

        int waitingHours = 1;

        //把任务开始时间转成成资源所在时区时间
        taskStartTime = startTimeParse(taskStartTime);
        System.out.println(taskStartTime);
        //计算deadline
        //true 超时,false 没超时
        boolean isOverTime = deadLineCalculator(taskStartTime, waitingHours, currentTime);
        System.out.println(isOverTime);

    }

    /**
     * 非工作时间开始的task,从工作时间开始计时
     * @param startTime 任务发布时间
     * @return 经过转换后的工作开始计时的时间
     * @throws ParseException
     */
    public static Date startTimeParse(Date startTime) throws ParseException {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startTime);
        int day = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        day = day == 0 ? 7 : day;
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (day > 5 || (day == 5 && hour >= 18)) {
            //周五18点之后分配的task,均设置成下周一上午9点开始计算
            calendar.set(Calendar.WEEK_OF_YEAR, calendar.getWeeksInWeekYear() + 1);
            calendar.set(Calendar.DAY_OF_WEEK, 2);
            return sdf2.parse(sdf1.format(calendar.getTime()) + " 09:00:00");
        } else if (hour >= 9 && hour < 18) {
            //工作时间发布的task
            return startTime;
        } else if (hour >= 18) {
            //工作日非工作时间发布的task
            startTime = DateUtils.addDays(startTime, 1);
            return sdf2.parse(sdf1.format(startTime) + " 09:00:00");
        } else {
            return sdf2.parse(sdf1.format(startTime) + " 09:00:00");
        }
    }


    /**
     * 计算DeadLine 均转换成资源当地时间再进行计算
     * @param taskStartTime 任务开始时间
     * @param waitingHours 等待时间
     * @param currentTime 现在时刻
     * @return true:超时; false:没超时
     * @throws ParseException
     */
    public static boolean deadLineCalculator(Date taskStartTime, int waitingHours, Date currentTime) throws ParseException {
        Calendar calendar = Calendar.getInstance();
        Date deadLine = taskStartTime;
        while (waitingHours >= 9) {
            deadLine = DateUtils.addDays(deadLine, 1);
            if ((deadLine.getDay() - 1) == 6) {
                deadLine = DateUtils.addDays(deadLine, 2);
            }
            waitingHours -= 9;
        }
        if (waitingHours == 0) {
            return deadLine.compareTo(currentTime) > 0;
        }

        deadLine = DateUtils.addHours(deadLine, waitingHours);

        calendar.setTime(deadLine);
        int weekDay = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        weekDay = weekDay == 0 ? 7 : weekDay;
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (weekDay > 5 || (weekDay == 5 && hour >= 18)) {
            deadLine = DateUtils.addHours(deadLine, 15 + 48);
        } else if (hour >= 18 || hour < 9) {
            deadLine = DateUtils.addDays(deadLine, 15);
        }

        System.out.println("截止时间:" + deadLine + ",现在时刻:" + currentTime);
        return deadLine.compareTo(currentTime) < 0;
    }

}
