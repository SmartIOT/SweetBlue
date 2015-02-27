package com.idevicesinc.sweetblue.annotations;

import java.lang.annotation.*;

/**
 * Classes marked with this {@link Annotation} have immutable internal state, or the apparent behavior of so.
 * Most of the time this means all private final members.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Immutable
{

}
