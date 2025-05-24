package org.clematis.logging;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import ch.qos.logback.classic.pattern.MDCConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Custom MDC Converter
 */
public class CustomMDCConverter extends MDCConverter {

    @Override
    public String convert(ILoggingEvent event) {
        final Map<String, String> mdcPropertyMap = event.getMDCPropertyMap();
        return (mdcPropertyMap == null || mdcPropertyMap.isEmpty()) ? StringUtils.EMPTY
            : generateOutput(mdcPropertyMap);
    }

    /**
     * if no key is specified, return all the values present in the MDC, in the
     * format "[k1::v1][k2::v2], ..."
     */
    private static String generateOutput(Map<String, String> mdcPropertyMap) {
        final StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : mdcPropertyMap.entrySet()) {
            sb.append('[').append(entry.getKey()).append("::").append(entry.getValue()).append(']');
        }
        return sb.toString();
    }
}