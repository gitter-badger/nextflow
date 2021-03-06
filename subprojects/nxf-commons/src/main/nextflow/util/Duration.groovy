/*
 * Copyright (c) 2013-2014, Centre for Genomic Regulation (CRG).
 * Copyright (c) 2013-2014, Paolo Di Tommaso and the respective authors.
 *
 *   This file is part of 'Nextflow'.
 *
 *   Nextflow is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Nextflow is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Nextflow.  If not, see <http://www.gnu.org/licenses/>.
 */

package nextflow.util
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.util.logging.Slf4j
import org.apache.commons.lang.time.DurationFormatUtils
/**
 * A simple time duration representation
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
@CompileStatic
@EqualsAndHashCode(includes = 'durationInMillis')
class Duration implements Comparable<Duration>, Serializable {

    static private final FORMAT = ~/(\d+)\s*([a-zA-Z]+)/

    static private final LEGACY = ~/(\d{1,2}):(\d{1,2}):(\d{1,2})/

    static private final List<String> MILLIS = ['ms','milli','millis']

    static private final List<String> SECONDS = ['s','sec','second','seconds']

    static private final List<String> MINUTES = ['m','min','minute','minutes']

    static private final List<String> HOURS = ['h','hour','hours']

    static private final List<String> DAYS = ['d','day','days']

    /**
     * Duration in millis
     */
    final long durationInMillis

    /**
     * Create e a duration object having the specified number of millis
     *
     * @param duration The duration as milliseconds
     */
    Duration(long duration) {
        assert duration>=0
        this.durationInMillis = duration
    }


    /**
     * Default constructor is required by Kryo serializer
     * Do not removed or use it directly
     */
    private Duration() { durationInMillis=0 }

    /**
     * Create the object using a string 'duration' format.
     * Accepted prefix are:
     * <li>{@code ms}, {@code milli}, {@code millis}: for milliseconds
     * <li>{@code s}, {@code second}, {@code seconds}: for seconds
     * <li>{@code m}, {@code minute}, {@code minutes}: for minutes
     * <li>{@code h}, {@code hour}, {@code hours}: for hours
     * <li>{@code d}, {@code day}, {@code days}: for days
     *
     *
     * @param str
     */
    Duration(String str) {

        try {
            try {
                durationInMillis = parseSimple(str)
            }
            catch( IllegalArgumentException e ) {
                durationInMillis = parseLegacy(str)
            }
        }
        catch( IllegalArgumentException e ) {
            throw e
        }
        catch( Exception e ) {
            throw new IllegalArgumentException("Not a valid duration value: ${str}", e)
        }
    }

    /**
     * Parse a duration string in legacy format i.e. hh:mm:ss
     *
     * @param str The string to be parsed e.g. {@code 05:10:30} (5 hours, 10 mins, 30 seconds)
     * @return The duration in millisecond
     */
    private long parseLegacy( String str ) {
        def matcher = (str =~ LEGACY)
        if( !matcher.matches() )
            new IllegalArgumentException("Not a valid duration value: ${str}")

        def groups = (List<String>)matcher[0]
        def hh = groups[1].toInteger()
        def mm = groups[2].toInteger()
        def ss = groups[3].toInteger()

        return TimeUnit.HOURS.toMillis(hh) + TimeUnit.MINUTES.toMillis(mm) + TimeUnit.SECONDS.toMillis(ss)
    }

    /**
     * Parse a duration string
     *
     * @param str A duration string containing one or more component e.g. {@code 1d 3h 10mins}
     * @return  The duration in millisecond
     */
    private long parseSimple( String str ) {

        long result=0
        for( int i=0; true; i++ ) {
            def matcher = (str =~ FORMAT)
            if( matcher.find() ) {
                def groups = (List<String>)matcher[0]
                def all = groups[0]
                def digit = groups[1]
                def unit = groups[2]

                result += convert( digit.toInteger(), unit )
                str = str.substring(all.length()).trim()
                continue
            }


            if( i == 0 )
                throw new IllegalArgumentException("Not a valid duration value: ${str}")
            break
        }

        return result
    }

    /**
     * Parse a single duration component
     *
     * @param digit
     * @param unit A valid duration unit e.g. {@code d}, {@code d}, {@code h}, {@code hour}, etc
     * @return The duration in millisecond
     */
    private long convert( int digit, String unit ) {

        if( unit in MILLIS ) {
            return digit
        }
        if ( unit in SECONDS ) {
            return TimeUnit.SECONDS.toMillis(digit)
        }
        if ( unit in MINUTES ) {
            return TimeUnit.MINUTES.toMillis(digit)
        }
        if ( unit in HOURS ) {
            return TimeUnit.HOURS.toMillis(digit)
        }
        if ( unit in DAYS ) {
            return TimeUnit.DAYS.toMillis(digit)
        }

        throw new IllegalStateException()
    }

    Duration(long value0, TimeUnit unit) {
        assert unit
        this.durationInMillis = unit.toMillis(value0)
    }

    static Duration of( long value ) {
        new Duration(value)
    }

    static Duration of( String str ) {
        new Duration(str)
    }

    static Duration of( String str, Duration fallback ) {
        try {
            return new Duration(str)
        }
        catch( IllegalArgumentException e ) {
            log.debug "Not a valid duration value: $str -- Fallback on default value: $fallback"
            return fallback
        }
    }

    long toMillis() {
        durationInMillis
    }

    long toSeconds() {
        TimeUnit.MILLISECONDS.toSeconds(durationInMillis)
    }

    long toMinutes() {
        TimeUnit.MILLISECONDS.toMinutes(durationInMillis)
    }

    long toHours() {
        TimeUnit.MILLISECONDS.toHours(durationInMillis)
    }

    long toDays() {
        TimeUnit.MILLISECONDS.toDays(durationInMillis)
    }

    /**
     * Duration formatting utilities and constants. The following table describes the tokens used in the pattern language for formatting.
     * <p>
     * <pre>
     *   character	duration element
     *   y	        years
     *   d	        days
     *   H	        hours
     *   m	        minutes
     *   s	        seconds
     * </pre>
     *
     * @param fmt
     * @return
     */
    String format( String fmt ) {
        DurationFormatUtils.formatDuration(durationInMillis, fmt)
    }

    String toString() {

        if( durationInMillis < 1000 ) {
            return durationInMillis + MILLIS[0]
        }

        def value = format("d:H:m:s").split(':').collect { String it -> Integer.parseInt(it) }
        def result = []

        // -- day / days
        if( value[0] >= 1 ) {
            result << value[0] + DAYS[0]
        }

        // hour / hours
        if( value[1] >= 1 ) {
            result << value[1] + HOURS[0]
        }

        // -- minute / minutes
        if( value[2] > 0 ) {
            result << value[2] + MINUTES[0]
        }

        // -- second / seconds
        if( value[3] > 0 ) {
            result << value[3] + SECONDS[0]
        }

        result.join(' ')
    }


    @Override
    int compareTo(Duration that) {
        return this.durationInMillis <=> that.durationInMillis
    }

    @EqualsAndHashCode
    static class ThrottleObj {
        Object result
        long timestamp

        ThrottleObj() {}

        ThrottleObj( value, long timestamp ) {
            this.result = value
            this.timestamp = timestamp
        }
    }

    def throttle( Closure closure ) {
        throttle0( durationInMillis, null, closure)
    }

    def throttle( seed, Closure closure ) {
        def initialValue = new ThrottleObj( seed, System.currentTimeMillis() )
        throttle0( durationInMillis, initialValue, closure)
    }

    static final Map<Integer,ThrottleObj> throttleMap = new ConcurrentHashMap<>()

    private static throttle0( long delayMillis, ThrottleObj initialValue, Closure closure ) {
        assert closure != null

        def key = 17
        key  = 31 * key + closure.class.hashCode()
        key  = 31 * key + closure.owner.hashCode()
        key  = 31 * key + closure.delegate?.hashCode() ?: 0

        ThrottleObj obj = throttleMap.get(key)
        if( obj == null ) {
            obj = initialValue ?: new ThrottleObj()
            throttleMap.put(key,obj)
        }

        if( System.currentTimeMillis() - obj.timestamp > delayMillis ) {
            obj.timestamp = System.currentTimeMillis()
            obj.result = closure.call()
        }

        obj.result
    }

}
