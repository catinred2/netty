/* 
 * Copyright 2001-2009 Terracotta, Inc. 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 *   
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations 
 * under the License.
 * 
 */

package io.netty.util.easytimer.quartz;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * <p>
 * A concrete <code>{@link Trigger}</code> that is used to fire a
 * <code>{@link org.quartz.JobDetail}</code> at given moments in time, defined
 * with Unix 'cron-like' definitions.
 * </p>
 * 
 * 
 * @author Sharada Jambula, James House
 * @author Contributions from Mads Henderson
 */
public class CronTriggerImpl {
    
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constants.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */
    
    /**
     * Required for serialization support. Introduced in Quartz 1.6.1 to
     * maintain compatibility after the introduction of hasAdditionalProperties
     * method.
     * 
     * @see java.io.Serializable
     */
    private static final long serialVersionUID = -8644953146451592766L;
    
    private int misfireInstruction = MISFIRE_INSTRUCTION_SMART_POLICY;
    
    public enum TriggerState {
        NONE, NORMAL, PAUSED, COMPLETE, ERROR, BLOCKED
    }
    
    /**
     * <p>
     * <code>NOOP</code> Instructs the <code>{@link Scheduler}</code> that the
     * <code>{@link Trigger}</code> has no further instructions.
     * </p>
     * 
     * <p>
     * <code>RE_EXECUTE_JOB</code> Instructs the <code>{@link Scheduler}</code>
     * that the <code>{@link Trigger}</code> wants the
     * <code>{@link org.quartz.JobDetail}</code> to re-execute immediately. If
     * not in a 'RECOVERING' or 'FAILED_OVER' situation, the execution context
     * will be re-used (giving the <code>Job</code> the ability to 'see'
     * anything placed in the context by its last execution).
     * </p>
     * 
     * <p>
     * <code>SET_TRIGGER_COMPLETE</code> Instructs the
     * <code>{@link Scheduler}</code> that the <code>{@link Trigger}</code>
     * should be put in the <code>COMPLETE</code> state.
     * </p>
     * 
     * <p>
     * <code>DELETE_TRIGGER</code> Instructs the <code>{@link Scheduler}</code>
     * that the <code>{@link Trigger}</code> wants itself deleted.
     * </p>
     * 
     * <p>
     * <code>SET_ALL_JOB_TRIGGERS_COMPLETE</code> Instructs the
     * <code>{@link Scheduler}</code> that all <code>Trigger</code>s referencing
     * the same <code>{@link org.quartz.JobDetail}</code> as this one should be
     * put in the <code>COMPLETE</code> state.
     * </p>
     * 
     * <p>
     * <code>SET_TRIGGER_ERROR</code> Instructs the
     * <code>{@link Scheduler}</code> that all <code>Trigger</code>s referencing
     * the same <code>{@link org.quartz.JobDetail}</code> as this one should be
     * put in the <code>ERROR</code> state.
     * </p>
     * 
     * <p>
     * <code>SET_ALL_JOB_TRIGGERS_ERROR</code> Instructs the
     * <code>{@link Scheduler}</code> that the <code>Trigger</code> should be
     * put in the <code>ERROR</code> state.
     * </p>
     */
    public enum CompletedExecutionInstruction {
        NOOP, RE_EXECUTE_JOB, SET_TRIGGER_COMPLETE, DELETE_TRIGGER, SET_ALL_JOB_TRIGGERS_COMPLETE, SET_TRIGGER_ERROR, SET_ALL_JOB_TRIGGERS_ERROR
    }
    
    /**
     * Instructs the <code>{@link Scheduler}</code> that upon a mis-fire
     * situation, the <code>updateAfterMisfire()</code> method will be called on
     * the <code>Trigger</code> to determine the mis-fire instruction, which
     * logic will be trigger-implementation-dependent.
     * 
     * <p>
     * In order to see if this instruction fits your needs, you should look at
     * the documentation for the <code>getSmartMisfirePolicy()</code> method on
     * the particular <code>Trigger</code> implementation you are using.
     * </p>
     */
    public static final int MISFIRE_INSTRUCTION_SMART_POLICY = 0;
    
    /**
     * Instructs the <code>{@link Scheduler}</code> that the
     * <code>Trigger</code> will never be evaluated for a misfire situation, and
     * that the scheduler will simply try to fire it as soon as it can, and then
     * update the Trigger as if it had fired at the proper time.
     * 
     * <p>
     * NOTE: if a trigger uses this instruction, and it has missed several of
     * its scheduled firings, then several rapid firings may occur as the
     * trigger attempt to catch back up to where it would have been. For
     * example, a SimpleTrigger that fires every 15 seconds which has misfired
     * for 5 minutes will fire 20 times once it gets the chance to fire.
     * </p>
     */
    public static final int MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY = -1;
    
    /**
     * <p>
     * Instructs the <code>{@link Scheduler}</code> that upon a mis-fire
     * situation, the <code>{@link CronTrigger}</code> wants to be fired now by
     * <code>Scheduler</code>.
     * </p>
     */
    public static final int MISFIRE_INSTRUCTION_FIRE_ONCE_NOW = 1;
    
    /**
     * <p>
     * Instructs the <code>{@link Scheduler}</code> that upon a mis-fire
     * situation, the <code>{@link CronTrigger}</code> wants to have it's
     * next-fire-time updated to the next time in the schedule after the current
     * time (taking into account any associated <code>{@link Calendar}</code>,
     * but it does not want to be fired now.
     * </p>
     */
    public static final int MISFIRE_INSTRUCTION_DO_NOTHING = 2;
    
    /**
     * The default value for priority.
     */
    public static final int DEFAULT_PRIORITY = 5;
    
    protected static final int YEAR_TO_GIVEUP_SCHEDULING_AT = CronExpression.MAX_YEAR;
    
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */
    
    private CronExpression cronEx = null;
    
    private Date startTime = null;
    
    private Date endTime = null;
    
    private Date nextFireTime = null;
    
    private Date previousFireTime = null;
    
    private transient TimeZone timeZone = null;
    
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constructors.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */
    
    /**
     * <p>
     * Create a <code>CronTrigger</code> with no settings.
     * </p>
     * 
     * <p>
     * The start-time will also be set to the current time, and the time zone
     * will be set the the system's default time zone.
     * </p>
     */
    public CronTriggerImpl() {
        super();
        setStartTime(new Date());
        setTimeZone(TimeZone.getDefault());
    }
    
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */
    
    public void setCronExpression(String cronExpression)
        throws ParseException {
        TimeZone origTz = getTimeZone();
        this.cronEx = new CronExpression(cronExpression);
        this.cronEx.setTimeZone(origTz);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.quartz.CronTriggerI#getCronExpression()
     */
    public String getCronExpression() {
        return cronEx == null ? null : cronEx.getCronExpression();
    }
    
    /**
     * Set the CronExpression to the given one. The TimeZone on the passed-in
     * CronExpression over-rides any that was already set on the Trigger.
     */
    public void setCronExpression(CronExpression cronExpression) {
        this.cronEx = cronExpression;
        this.timeZone = cronExpression.getTimeZone();
    }
    
    /**
     * <p>
     * Get the time at which the <code>CronTrigger</code> should occur.
     * </p>
     */
    public Date getStartTime() {
        return this.startTime;
    }
    
    public void setStartTime(Date startTime) {
        if (startTime == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }
        
        Date eTime = getEndTime();
        if (eTime != null && eTime.before(startTime)) {
            throw new IllegalArgumentException("End time cannot be before start time");
        }
        
        // round off millisecond...
        // Note timeZone is not needed here as parameter for
        // Calendar.getInstance(),
        // since time zone is implicit when using a Date in the setTime method.
        Calendar cl = Calendar.getInstance();
        cl.setTime(startTime);
        cl.set(Calendar.MILLISECOND, 0);
        
        this.startTime = cl.getTime();
    }
    
    /**
     * <p>
     * Get the time at which the <code>CronTrigger</code> should quit repeating
     * - even if repeastCount isn't yet satisfied.
     * </p>
     * 
     * @see #getFinalFireTime()
     */
    public Date getEndTime() {
        return this.endTime;
    }
    
    public void setEndTime(Date endTime) {
        Date sTime = getStartTime();
        if (sTime != null && endTime != null && sTime.after(endTime)) {
            throw new IllegalArgumentException("End time cannot be before start time");
        }
        
        this.endTime = endTime;
    }
    
    /**
     * <p>
     * Returns the next time at which the <code>Trigger</code> is scheduled to
     * fire. If the trigger will not fire again, <code>null</code> will be
     * returned. Note that the time returned can possibly be in the past, if the
     * time that was computed for the trigger to next fire has already arrived,
     * but the scheduler has not yet been able to fire the trigger (which would
     * likely be due to lack of resources e.g. threads).
     * </p>
     * 
     * <p>
     * The value returned is not guaranteed to be valid until after the
     * <code>Trigger</code> has been added to the scheduler.
     * </p>
     * 
     * @see TriggerUtils#computeFireTimesBetween(org.quartz.spi.OperableTrigger,
     *      org.EasyTimerCalendar.Calendar, java.util.Date, java.util.Date)
     */
    public Date getNextFireTime() {
        return this.nextFireTime;
    }
    
    /**
     * <p>
     * Returns the previous time at which the <code>CronTrigger</code> fired. If
     * the trigger has not yet fired, <code>null</code> will be returned.
     */
    public Date getPreviousFireTime() {
        return this.previousFireTime;
    }
    
    /**
     * <p>
     * Sets the next time at which the <code>CronTrigger</code> will fire.
     * <b>This method should not be invoked by client code.</b>
     * </p>
     */
    public void setNextFireTime(Date nextFireTime) {
        this.nextFireTime = nextFireTime;
    }
    
    /**
     * <p>
     * Set the previous time at which the <code>CronTrigger</code> fired.
     * </p>
     * 
     * <p>
     * <b>This method should not be invoked by client code.</b>
     * </p>
     */
    public void setPreviousFireTime(Date previousFireTime) {
        this.previousFireTime = previousFireTime;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.quartz.CronTriggerI#getTimeZone()
     */
    public TimeZone getTimeZone() {
        
        if (cronEx != null) {
            return cronEx.getTimeZone();
        }
        
        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
        }
        return timeZone;
    }
    
    /**
     * <p>
     * Sets the time zone for which the <code>cronExpression</code> of this
     * <code>CronTrigger</code> will be resolved.
     * </p>
     * 
     * <p>
     * If {@link #setCronExpression(CronExpression)} is called after this
     * method, the TimeZon setting on the CronExpression will "win". However if
     * {@link #setCronExpression(String)} is called after this method, the time
     * zone applied by this method will remain in effect, since the String cron
     * expression does not carry a time zone!
     */
    public void setTimeZone(TimeZone timeZone) {
        if (cronEx != null) {
            cronEx.setTimeZone(timeZone);
        }
        this.timeZone = timeZone;
    }
    
    /**
     * <p>
     * Returns the next time at which the <code>CronTrigger</code> will fire,
     * after the given time. If the trigger will not fire after the given time,
     * <code>null</code> will be returned.
     * </p>
     * 
     * <p>
     * Note that the date returned is NOT validated against the related
     * org.quartz.Calendar (if any)
     * </p>
     */
    public Date getFireTimeAfter(Date afterTime) {
        if (afterTime == null) {
            afterTime = new Date();
        }
        
        if (getStartTime().after(afterTime)) {
            afterTime = new Date(getStartTime().getTime() - 1000l);
        }
        
        if (getEndTime() != null && (afterTime.compareTo(getEndTime()) >= 0)) {
            return null;
        }
        
        Date pot = getTimeAfter(afterTime);
        if (getEndTime() != null && pot != null && pot.after(getEndTime())) {
            return null;
        }
        
        return pot;
    }
    
    /**
     * <p>
     * NOT YET IMPLEMENTED: Returns the final time at which the
     * <code>CronTrigger</code> will fire.
     * </p>
     * 
     * <p>
     * Note that the return time *may* be in the past. and the date returned is
     * not validated against org.quartz.calendar
     * </p>
     */
    public Date getFinalFireTime() {
        Date resultTime;
        if (getEndTime() != null) {
            resultTime = getTimeBefore(new Date(getEndTime().getTime() + 1000l));
        } else {
            resultTime = (cronEx == null) ? null : cronEx.getFinalFireTime();
        }
        
        if ((resultTime != null) && (getStartTime() != null) && (resultTime.before(getStartTime()))) {
            return null;
        }
        
        return resultTime;
    }
    
    /**
     * <p>
     * Determines whether or not the <code>CronTrigger</code> will occur again.
     * </p>
     */
    public boolean mayFireAgain() {
        return (getNextFireTime() != null);
    }
    
    protected boolean validateMisfireInstruction(int misfireInstruction) {
        return misfireInstruction >= MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY && misfireInstruction <= MISFIRE_INSTRUCTION_DO_NOTHING;
    }
    
    /**
     * <p>
     * Updates the <code>CronTrigger</code>'s state based on the
     * MISFIRE_INSTRUCTION_XXX that was selected when the
     * <code>CronTrigger</code> was created.
     * </p>
     * 
     * <p>
     * If the misfire instruction is set to MISFIRE_INSTRUCTION_SMART_POLICY,
     * then the following scheme will be used: <br>
     * <ul>
     * <li>The instruction will be interpreted as
     * <code>MISFIRE_INSTRUCTION_FIRE_ONCE_NOW</code>
     * </ul>
     * </p>
     */
    public void updateAfterMisfire(EasyTimerCalendar cal) {
        int instr = getMisfireInstruction();
        
        if (instr == MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY) return;
        
        if (instr == MISFIRE_INSTRUCTION_SMART_POLICY) {
            instr = MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;
        }
        
        if (instr == MISFIRE_INSTRUCTION_DO_NOTHING) {
            Date newFireTime = getFireTimeAfter(new Date());
            while (newFireTime != null && cal != null && !cal.isTimeIncluded(newFireTime.getTime())) {
                newFireTime = getFireTimeAfter(newFireTime);
            }
            setNextFireTime(newFireTime);
        } else if (instr == MISFIRE_INSTRUCTION_FIRE_ONCE_NOW) {
            setNextFireTime(new Date());
        }
    }
    
    /**
     * <p>
     * Determines whether the date and (optionally) time of the given Calendar
     * instance falls on a scheduled fire-time of this trigger.
     * </p>
     * 
     * <p>
     * Equivalent to calling <code>willFireOn(cal, false)</code>.
     * </p>
     * 
     * @param test the date to compare
     * 
     * @see #willFireOn(Calendar, boolean)
     */
    public boolean willFireOn(Calendar test) {
        return willFireOn(test, false);
    }
    
    /**
     * <p>
     * Determines whether the date and (optionally) time of the given Calendar
     * instance falls on a scheduled fire-time of this trigger.
     * </p>
     * 
     * <p>
     * Note that the value returned is NOT validated against the related
     * org.quartz.Calendar (if any)
     * </p>
     * 
     * @param test the date to compare
     * @param dayOnly if set to true, the method will only determine if the
     *            trigger will fire during the day represented by the given
     *            Calendar (hours, minutes and seconds will be ignored).
     * @see #willFireOn(Calendar)
     */
    public boolean willFireOn(Calendar test, boolean dayOnly) {
        
        test = (Calendar)test.clone();
        
        test.set(Calendar.MILLISECOND, 0); // don't compare millis.
        
        if (dayOnly) {
            test.set(Calendar.HOUR_OF_DAY, 0);
            test.set(Calendar.MINUTE, 0);
            test.set(Calendar.SECOND, 0);
        }
        
        Date testTime = test.getTime();
        
        Date fta = getFireTimeAfter(new Date(test.getTime().getTime() - 1000));
        
        if (fta == null) return false;
        
        Calendar p = Calendar.getInstance(test.getTimeZone());
        p.setTime(fta);
        
        int year = p.get(Calendar.YEAR);
        int month = p.get(Calendar.MONTH);
        int day = p.get(Calendar.DATE);
        
        if (dayOnly) {
            return (year == test.get(Calendar.YEAR) && month == test.get(Calendar.MONTH) && day == test.get(Calendar.DATE));
        }
        
        while (fta.before(testTime)) {
            fta = getFireTimeAfter(fta);
        }
        
        return fta.equals(testTime);
    }
    
    /**
     * <p>
     * Called when the <code>{@link Scheduler}</code> has decided to 'fire' the
     * trigger (execute the associated <code>Job</code>), in order to give the
     * <code>Trigger</code> a chance to update itself for its next triggering
     * (if any).
     * </p>
     * 
     * @see #executionComplete(JobExecutionContext, JobExecutionException)
     */
    public void triggered(EasyTimerCalendar calendar) {
        previousFireTime = nextFireTime;
        nextFireTime = getFireTimeAfter(nextFireTime);
        
        while (nextFireTime != null && calendar != null && !calendar.isTimeIncluded(nextFireTime.getTime())) {
            nextFireTime = getFireTimeAfter(nextFireTime);
        }
    }
    
    /**
     * 
     * @see AbstractTrigger#updateWithNewCalendar(org.EasyTimerCalendar.Calendar, long)
     */
    public void updateWithNewCalendar(EasyTimerCalendar calendar, long misfireThreshold) {
        nextFireTime = getFireTimeAfter(previousFireTime);
        
        if (nextFireTime == null || calendar == null) {
            return;
        }
        
        Date now = new Date();
        while (nextFireTime != null && !calendar.isTimeIncluded(nextFireTime.getTime())) {
            
            nextFireTime = getFireTimeAfter(nextFireTime);
            
            if (nextFireTime == null) break;
            
            // avoid infinite loop
            // Use gregorian only because the constant is based on Gregorian
            java.util.Calendar c = new java.util.GregorianCalendar();
            c.setTime(nextFireTime);
            if (c.get(java.util.Calendar.YEAR) > YEAR_TO_GIVEUP_SCHEDULING_AT) {
                nextFireTime = null;
            }
            
            if (nextFireTime != null && nextFireTime.before(now)) {
                long diff = now.getTime() - nextFireTime.getTime();
                if (diff >= misfireThreshold) {
                    nextFireTime = getFireTimeAfter(nextFireTime);
                }
            }
        }
    }
    
    /**
     * <p>
     * Called by the scheduler at the time a <code>Trigger</code> is first added
     * to the scheduler, in order to have the <code>Trigger</code> compute its
     * first fire time, based on any associated calendar.
     * </p>
     * 
     * <p>
     * After this method has been called, <code>getNextFireTime()</code> should
     * return a valid answer.
     * </p>
     * 
     * @return the first time at which the <code>Trigger</code> will be fired by
     *         the scheduler, which is also the same value
     *         <code>getNextFireTime()</code> will return (until after the first
     *         firing of the <code>Trigger</code>). </p>
     */
    public Date computeFirstFireTime(EasyTimerCalendar calendar) {
        nextFireTime = getFireTimeAfter(new Date(getStartTime().getTime() - 1000l));
        
        while (nextFireTime != null && calendar != null && !calendar.isTimeIncluded(nextFireTime.getTime())) {
            nextFireTime = getFireTimeAfter(nextFireTime);
        }
        
        return nextFireTime;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.quartz.CronTriggerI#getExpressionSummary()
     */
    public String getExpressionSummary() {
        return cronEx == null ? null : cronEx.getExpressionSummary();
    }
    
    /**
     * Used by extensions of CronTrigger to imply that there are additional
     * properties, specifically so that extensions can choose whether to be
     * stored as a serialized blob, or as a flattened CronTrigger table.
     */
    public boolean hasAdditionalProperties() {
        return false;
    }
    
    // //////////////////////////////////////////////////////////////////////////
    //
    // Computation Functions
    //
    // //////////////////////////////////////////////////////////////////////////
    
    protected Date getTimeAfter(Date afterTime) {
        return (cronEx == null) ? null : cronEx.getTimeAfter(afterTime);
    }
    
    /**
     * NOT YET IMPLEMENTED: Returns the time before the given time that this
     * <code>CronTrigger</code> will fire.
     */
    protected Date getTimeBefore(Date eTime) {
        return (cronEx == null) ? null : cronEx.getTimeBefore(eTime);
    }
    
    /**
     * @return the misfireInstruction
     */
    public int getMisfireInstruction() {
        return misfireInstruction;
    }
    
    /**
     * @param misfireInstruction the misfireInstruction to set
     */
    public void setMisfireInstruction(int misfireInstruction) {
        this.misfireInstruction = misfireInstruction;
    }
    
}
