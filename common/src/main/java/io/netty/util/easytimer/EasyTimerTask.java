/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.util.easytimer;

import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import io.netty.util.easytimer.quartz.CronExpression;

import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * A task which is executed after the delay specified with
 * {@link EasyTimer#newTimeout(TimerTask, long, TimeUnit)}.
 */
public abstract class EasyTimerTask implements TimerTask {
    
    String name;
    
    EasyHashedWheelTimer hashedWheelTimer;
    
    CronExpression cronExpression;
    
    public EasyTimerTask(String cronExpression, String name) throws ParseException {
        this.cronExpression = new CronExpression(cronExpression);
        this.name = name;
    }
    
    @Override
    public abstract void run(Timeout timeout)
        throws Exception;
    
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    
    public long nextTriggerTime() {
        Date validDate = cronExpression.getNextValidTimeAfter(new Date());
        if (validDate == null) {
            return -1L;
        }
        return validDate.getTime();
    }
    
    
    
}
